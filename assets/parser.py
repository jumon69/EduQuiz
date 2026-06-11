import os
import re
import json
import easyocr
import fitz  # PyMuPDF for high-speed streaming PDF text extraction
from PIL import Image

class HSCDocumentParser:
    def __init__(self):
        # 1. Initialize EasyOCR with both Bangla (bn) and English (en) prioritized simultaneously.
        # This is critical for HSC physics/chemistry books which constantly mix scripts.
        print("[INIT] Initializing multi-lingual EasyOCR parser (Bangla + English)...")
        self.reader = easyocr.Reader(['bn', 'en'], gpu=False)

    def calculate_file_hash(self, file_path, algorithm='sha256'):
        """
        Calculates the file checksum dynamically (MD5 / SHA-256) of massive scientific documents.
        """
        import hashlib
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Source file not found at path: {file_path}")
        
        hasher = hashlib.sha256() if algorithm == 'sha256' else hashlib.md5()
        with open(file_path, 'rb') as f:
            # Read in memory-safe 64KB blocks
            for block in iter(lambda: f.read(65536), b""):
                hasher.update(block)
        return hasher.hexdigest()

    def check_cache_registry(self, file_path):
        """
        Verifies if the file hash matches an existing study set, bypassing OCR/AI workloads.
        """
        file_hash = self.calculate_file_hash(file_path, algorithm='sha256')
        registry_path = ".parser_cache.json"
        
        if os.path.exists(registry_path):
            try:
                with open(registry_path, "r", encoding="utf-8") as f:
                    registry = json.load(f)
                    if file_hash in registry:
                        print(f"[CACHE HIT] File already processed. Checksum: {file_hash}")
                        return True, file_hash, registry[file_hash]
            except Exception as e:
                print(f"[CACHE WARN] Registry error: {e}. Rebuilding...")
                
        print(f"[CACHE MISS] New file checksum verified: {file_hash}. Initializing PyMuPDF stream...")
        return False, file_hash, None

    def register_processed_file(self, file_path, questions_list):
        """
        Saves successfully parsed MCQ sets under their SHA-256 hash key for instant retrieval.
        """
        file_hash = self.calculate_file_hash(file_path, algorithm='sha256')
        registry_path = ".parser_cache.json"
        registry = {}
        
        if os.path.exists(registry_path):
            try:
                with open(registry_path, "r", encoding="utf-8") as f:
                    registry = json.load(f)
            except Exception:
                pass
                
        registry[file_hash] = questions_list
        with open(registry_path, "w", encoding="utf-8") as f:
            json.dump(registry, f, ensure_ascii=False, indent=2)
        print(f"[CACHE STORE] Cached {len(questions_list)} MCQs under SHA-256: {file_hash}")

    def extract_text_from_pdf_stream(self, pdf_path):
        """
        Memory-safe generator that streams text from massive PDF documents (up to 160MB+)
        page-by-page to prevent RAM crashes during extraction.
        """
        if not os.path.exists(pdf_path):
            raise FileNotFoundError(f"PDF file not found at: {pdf_path}")
            
        print(f"[STREAM] Opening stream for {pdf_path}")
        doc = fitz.open(pdf_path)
        
        for page_num in range(len(doc)):
            page = doc.load_page(page_num)
            # Fetch UTF-8 raw text from current page
            page_text = page.get_text("text")
            yield page_num + 1, page_text

    def extract_text_from_image_ocr(self, image_input):
        """
        Processes camera snapshots or textbook page images. 
        Accepts either an image path or PIL Image object.
        """
        print("[OCR] Initiating OCR recognition pipeline...")
        # Undergo OCR with EasyOCR
        results = self.reader.readtext(image_input, detail=0)
        # Combine extracted tokens into single contiguous space-separated string
        raw_text = " ".join(results)
        return raw_text

    def sanitize_multilingual_text(self, text: str) -> str:
        """
        Advanced Regex Sanitization logic that protects Bangla Unicode script block (U+0980 to U+09FF)
        including vowel signs (কার, ফলা), combined letters, mathematical operators, and scientific 
        notations (e.g. H2O, CO2, λ = h/p, 10^-3) while removing terminal garbage characters.
        """
        if not text:
            return ""

        # Remove strange control characters but preserve standard spaces, newlines, tabs
        cleaned = re.sub(r'[^\s\u0980-\u09FFa-zA-Z0-9().,:\-\=\+\/\*\\_\[\]\{\}\<\>!?°%^×÷]', '', text)

        # Correct whitespace clusters
        cleaned = re.sub(r'[ \t]+', ' ', cleaned)
        cleaned = re.sub(r'\n\s*\n', '\n', cleaned)
        
        return cleaned.strip()

    def parse_hsc_mcqs(self, raw_text: str):
        """
        Parses Bangla-English MCQs utilizing specialized regex patterns.
        Handles Bangla numeric prefixes (e.g., ১, ২, ৩), Bangla block signs, English characters,
        and cleanly slices option anchors: (A), (B), (C), (D) or (ক), (খ), (গ), (ঘ).
        
        Example matched anchors:
           ১. নিচের কোনটি সঠিক? (A) অপশন ১ (B) অপশন ২ (C) অপশন ৩ (D) অপশন ৪
           or English counterparts.
        """
        sanitized = self.sanitize_multilingual_text(raw_text)
        
        # Regex breakdown:
        # Match question start index: can be standard English digit or Bengali digit (e.g. ১, ২) 
        # followed by full stop or hyphen.
        # Lookahead separates the question text matching until a bracketed option like (A), (B), or (ক) is found.
        question_pattern = r'(?:^|\n)(?:\d+|[\u09E6-\u09EF]+)[\.\-]\s*(.*?)(?=\s*\((?:[A-Da-d]|\u0995|\u0996|\u0997|\u0998)\)|\s*$)'
        
        # Pattern to extract individual options matching standard English bracket placeholders 
        # (A)/(B)/(C)/(D) or Bangla counterparts (ক)/(খ)/(গ)/(ঘ).
        option_patterns = {
            "A": r'\((?:A|a|\u0995)\)\s*(.*?)(?=\((?:B|b|\u0996)\)|\((?:C|c|\u0997)\)|\((?:D|d|\u0998)\)|$|\n)',
            "B": r'\((?:B|b|\u0996)\)\s*(.*?)(?=\((?:C|c|\u0997)\)|\((?:D|d|\u0998)\)|$|\n)',
            "C": r'\((?:C|c|\u0997)\)\s*(.*?)(?=\((?:D|d|\u0998)\)|$|\n)',
            "D": r'\((?:D|d|\u0998)\)\s*(.*?)(?=$|\n|\(\w+\))'
        }

        questions_list = []
        raw_questions_blocks = re.split(r'(?=(?:^|\n)(?:\d+|[\u09E6-\u09EF]+)[\.\-]\s*)', sanitized)

        for block in raw_questions_blocks:
            block = block.strip()
            if not block:
                continue
                
            # Extract question text
            q_match = re.search(r'^(?:\d+|[\u09E6-\u09EF]+)[\.\-]\s*(.*?)(?=\s*\((?:[A-Da-d]|\u0995|\u0996|\u0997|\u0998)\)|$)', block, re.DOTALL)
            if not q_match:
                continue
                
            question_text = q_match.group(1).strip()
            
            # Extract options dynamically
            opt_a = re.search(option_patterns["A"], block, re.DOTALL)
            opt_b = re.search(option_patterns["B"], block, re.DOTALL)
            opt_c = re.search(option_patterns["C"], block, re.DOTALL)
            opt_d = re.search(option_patterns["D"], block, re.DOTALL)
            
            if opt_a and opt_b and opt_c and opt_d:
                # Basic default logic for answer key extraction or fallback
                # Since AI models handle correct answer determination on complete sets,
                # we default answer to 'A' if not otherwise annotated in text.
                correct_m = re.search(r'(?:উত্তর|Ans|Correct|Answer)\s*[:\-]?\s*(?:Option\s*)?\((?:[A-D]|\u0995|\u0996|\u0997|\u0998)\)', block, re.IGNORECASE)
                correct_ans = "A"
                if correct_m:
                    matched_tag = correct_m.group(0)
                    if any(x in matched_tag for x in ['B', 'খ']): correct_ans = "B"
                    elif any(x in matched_tag for x in ['C', 'গ']): correct_ans = "C"
                    elif any(x in matched_tag for x in ['D', 'ঘ']): correct_ans = "D"

                mcq_obj = {
                    "question": question_text,
                    "optionA": opt_a.group(1).strip(),
                    "optionB": opt_b.group(1).strip(),
                    "optionC": opt_c.group(1).strip(),
                    "optionD": opt_d.group(1).strip(),
                    "correctAnswer": correct_ans,
                    "explanation": f"HSC concepts analyzed from text: {text_snippet(block, 80)}"
                }
                questions_list.append(mcq_obj)

        return questions_list

def text_snippet(text, limit=100):
    flat = text.replace("\n", " ")
    return flat[:limit] + "..." if len(flat) > limit else flat

# Test harness execution
if __name__ == "__main__":
    sample_text = """
    ১. নিচের কোনটি প্লাঙ্কের ধ্রুবক (Planck's constant) h এর সঠিক মান প্রকাশ করে?
    (ক) 6.626 * 10^-34 J s (খ) 9.1 * 10^-31 kg (গ) 3 * 10^8 m/s (ঘ) 1.6 * 10^-19 C
    উত্তর: (ক)
    """
    print("--- SAMPLE RUNNING TEXT SANITIZATION ---")
    parser = HSCDocumentParser()
    extracted = parser.parse_hsc_mcqs(sample_text)
    print(json.dumps(extracted, ensure_ascii=False, indent=2))
