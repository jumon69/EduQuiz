from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import tempfile
import shutil
import os
from typing import List
import json
import re

try:
    from assets.parser import HSCDocumentParser
except Exception:
    # If run from package root, attempt relative import
    from ..assets.parser import HSCDocumentParser  # type: ignore

app = FastAPI(title="EduQuiz AI Engine")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

parser = HSCDocumentParser()

# Gemini setup will be done lazily per-request to avoid import errors if API key missing
try:
    import google.generativeai as genai
except Exception:
    genai = None


@app.post('/process-pdf')
async def process_pdf(file: UploadFile = File(...), subject_name: str = Form(...)) -> JSONResponse:
    if file.content_type != 'application/pdf':
        raise HTTPException(status_code=400, detail='Uploaded file must be a PDF')

    # Stream upload to a temporary file on disk to avoid large memory usage
    tmp = None
    try:
        tmp = tempfile.NamedTemporaryFile(delete=False, suffix='.pdf')
        # read in chunks and write to disk
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk:
                break
            tmp.write(chunk)
        tmp.flush()
        tmp.close()

        results = []
        # Use the streaming page-by-page extractor to parse without loading whole PDF in memory
        import re

        def ensure_prefix(opt_text: str, bangla_label: str) -> str:
            if not opt_text:
                return f"{bangla_label}"
            # if already starts with a Bangla anchor like 'ক' or contains ')' near start, keep
            if re.match(r'^\s*[\u0995-\u0998]', opt_text) or re.match(r'^\s*\(?[A-Da-d]\)?', opt_text):
                return opt_text.strip()
            return f"{bangla_label} {opt_text.strip()}"

        for page_num, page_text in parser.extract_text_from_pdf_stream(tmp.name):
            page_questions = parser.parse_hsc_mcqs(page_text)
            if page_questions:
                # attach source metadata minimally and normalize output shape
                for q in page_questions:
                    q.setdefault('sourcePage', page_num)
                    q.setdefault('subject', subject_name)

                    # Build options array in Bangla-labeled format
                    optA = q.get('optionA') or q.get('A') or ''
                    optB = q.get('optionB') or q.get('B') or ''
                    optC = q.get('optionC') or q.get('C') or ''
                    optD = q.get('optionD') or q.get('D') or ''

                    options = [
                        ensure_prefix(optA, 'ক)'),
                        ensure_prefix(optB, 'খ)'),
                        ensure_prefix(optC, 'গ)'),
                        ensure_prefix(optD, 'ঘ)'),
                    ]

                    formatted = {
                        'question': q.get('question') or q.get('text') or '',
                        'options': options,
                        'correctAnswer': q.get('correctAnswer', 'A'),
                        'sourcePage': q.get('sourcePage'),
                        'subject': q.get('subject'),
                    }
                    results.append(formatted)

        # Optionally cache registration (non-blocking)
        try:
            if results:
                # register by path - parser handles hashing
                parser.register_processed_file(tmp.name, results)
        except Exception:
            pass

        return JSONResponse(content=results)

    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
    finally:
        try:
            if tmp is not None:
                os.unlink(tmp.name)
        except Exception:
            pass


if __name__ == '__main__':
        # Gather page-by-page raw text (streamed)
        pages_text = []
        for page_num, page_text in parser.extract_text_from_pdf_stream(tmp.name):
            if page_text and page_text.strip():
                pages_text.append((page_num, page_text))

        if not pages_text:
            return JSONResponse(content=[], status_code=200)

        # Prepare Gemini client
        gemini_api_key = os.environ.get('GEMINI_API_KEY')
        if not gemini_api_key:
            raise HTTPException(status_code=500, detail='GEMINI_API_KEY not configured in environment')

        if genai is None:
            try:
                import google.generativeai as genai_local
                genai = genai_local
            except Exception as e:
                raise HTTPException(status_code=500, detail=f'google.generativeai library not available: {e}')

        genai.configure(api_key=gemini_api_key)

        # Strict system instruction as requested
        system_instruction = (
            "You are an advanced HSC Science Exam Parser. Scan the provided raw text from the PDF. "
            "Your task is to identify, select, and extract ONLY the academic multiple-choice questions (MCQs). "
            "Ignore page numbers, publisher info, and headings. Extract the question text, the 4 distinct options, "
            "and deduce the correct answer. You must output the result strictly as a valid JSON array of objects, "
            "with NO markdown formatting, NO ```json blocks, just the pure raw JSON array using this exact schema structure:\n"
            "[\n"
            "  {\n"
            "    \"question\": \"সম্পূর্ণ বাংলা প্রশ্ন...\",\n"
            "    \"options\": [\"ক) ...\", \"খ) ...\", \"গ) ...\", \"ঘ) ...\"],\n"
            "    \"correctAnswer\": \"ক\"\n"
            "  }\n"
            "]"
        )

        # Build a single concatenated text with page separators but keep chunks sized to avoid model limits
        MAX_CHARS = 120000
        chunks = []
        current = ''
        for page_num, txt in pages_text:
            piece = f"\n\n-- PAGE {page_num} --\n\n{txt.strip()}"
            if len(current) + len(piece) > MAX_CHARS and current:
                chunks.append(current)
                current = piece
            else:
                current += piece
        if current:
            chunks.append(current)

        aggregated_results = []

        def extract_json_array_from_text(s: str):
            # Find the first '[' and last ']' and extract substring
            start = s.find('[')
            end = s.rfind(']')
            if start == -1 or end == -1 or end <= start:
                return None
            candidate = s[start:end+1]
            try:
                return json.loads(candidate)
            except Exception:
                return None

        for chunk in chunks:
            prompt = system_instruction + "\n\nPDF_TEXT:\n" + chunk
            try:
                resp = genai.generate(model="gemini-1.5-flash", prompt=prompt, temperature=0)
                resp_text = ''
                # genai response object may expose text attribute
                if hasattr(resp, 'text'):
                    resp_text = resp.text
                else:
                    # safe fallback: try to stringify the object
                    resp_text = str(resp)

                # Extract JSON array substring and parse
                parsed = extract_json_array_from_text(resp_text)
                if parsed is None:
                    # try direct json.loads (in case model returned raw array)
                    try:
                        parsed = json.loads(resp_text)
                    except Exception:
                        raise HTTPException(status_code=502, detail='Gemini returned malformed JSON')

                # Validate and normalize each entry
                for item in parsed:
                    q = item.get('question') or item.get('Question') or ''
                    opts = item.get('options') or item.get('Choices') or []
                    ans = item.get('correctAnswer') or item.get('answer') or ''

                    # Normalize options to Bangla-labeled strings
                    def ensure_bangla_label(opt_text, label):
                        opt_text = (opt_text or '').strip()
                        if not opt_text:
                            return f"{label}"
                        if re.match(r'^\s*[\u0995-\u0998]', opt_text) or re.match(r'^\s*[A-Da-d]', opt_text):
                            return opt_text
                        return f"{label} {opt_text}"

                    normalized_options = []
                    labels = ['ক)', 'খ)', 'গ)', 'ঘ)']
                    for i in range(4):
                        text_opt = opts[i] if i < len(opts) else ''
                        normalized_options.append(ensure_bangla_label(text_opt, labels[i]))

                    # Normalize answer to single Bangla character
                    def normalize_answer(a: str) -> str:
                        a = (a or '').strip()
                        if not a:
                            return 'ক'
                        map_eng = {'A': 'ক', 'B': 'খ', 'C': 'গ', 'D': 'ঘ', 'a': 'ক', 'b': 'খ', 'c': 'গ', 'd': 'ঘ'}
                        if a in map_eng:
                            return map_eng[a]
                        if a.startswith('ক') or a == 'ক' or a.startswith('ক)'):
                            return 'ক'
                        if a.startswith('খ') or a == 'খ' or a.startswith('খ)'):
                            return 'খ'
                        if a.startswith('গ') or a == 'গ' or a.startswith('গ)'):
                            return 'গ'
                        if a.startswith('ঘ') or a == 'ঘ' or a.startswith('ঘ)'):
                            return 'ঘ'
                        # fallback: if contains letters A-D
                        for ch in a:
                            if ch.upper() in map_eng:
                                return map_eng[ch.upper()]
                        return 'ক'

                    normalized_answer = normalize_answer(ans)

                    aggregated_results.append({
                        'question': q,
                        'options': normalized_options,
                        'correctAnswer': normalized_answer,
                        'sourcePage': item.get('sourcePage'),
                        'subject': subject_name,
                    })
            except HTTPException:
                raise
            except Exception as exc:
                raise HTTPException(status_code=502, detail=f'Gemini call failed: {exc}')

        # Optionally cache the aggregated results
        try:
            if aggregated_results:
                # register_processed_file expects a file path and questions
                parser.register_processed_file(tmp.name, aggregated_results)
        except Exception:
            pass

        return JSONResponse(content=aggregated_results)
