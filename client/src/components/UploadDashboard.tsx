import React, { useRef, useState } from 'react'

const apiFallback = 'http://127.0.0.1:8000/process-pdf'

export const UploadDashboard: React.FC = () => {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [fileName, setFileName] = useState<string>('')
  const [subjectName, setSubjectName] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(false)
  const [quiz, setQuiz] = useState<any[]>([])
  const apiUrl = (import.meta as any).env?.VITE_API_URL || apiFallback

  function onChooseClick() {
    inputRef.current?.click()
  }

  function onFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files && e.target.files[0]
    if (f) {
      setSelectedFile(f)
      setFileName(f.name)
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!selectedFile) return alert('অনুগ্রহ করে একটি পিডিএফ ফাইল সিলেক্ট করুন।')

    const fd = new FormData()
    fd.append('file', selectedFile)
    fd.append('subject_name', subjectName || '')

    try {
      setLoading(true)
      setQuiz([])
      const res = await fetch(apiUrl, {
        method: 'POST',
        body: fd,
      })

      if (!res.ok) {
        const txt = await res.text()
        throw new Error(`Server error: ${res.status} ${txt}`)
      }

      const data = await res.json()
      if (!Array.isArray(data)) {
        throw new Error('Invalid response format from server')
      }

      setQuiz(data)
    } catch (err: any) {
      console.error(err)
      alert('পিডিএফ প্রসেসিং বা সার্ভার বিষয়ক সমস্যা: ' + (err.message || err))
    } finally {
      setLoading(false)
    }
  }

  const neonStyle: React.CSSProperties = {
    background: 'linear-gradient(90deg,#0ff,#f0f,#0ff)',
    color: '#0a0a0a',
    padding: '12px 20px',
    borderRadius: 12,
    border: 'none',
    fontWeight: 700,
    boxShadow: '0 6px 18px rgba(240,0,255,0.18), inset 0 -2px 8px rgba(0,255,255,0.06)'
  }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto', padding: 20 }}>
      <form onSubmit={onSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: 'block', marginBottom: 8 }}>TARGET_SUBJECT_NAME (বিষয় নাম)</label>
          <input
            style={{ width: '100%', padding: 8, borderRadius: 8, border: '1px solid #ccc' }}
            value={subjectName}
            onChange={(e) => setSubjectName(e.target.value)}
            placeholder="বিষয়ের নাম লিখুন (উদাহরণ: গণিত, পদার্থ)"
          />
        </div>

        <input
          id="pdf-file-picker"
          ref={inputRef}
          type="file"
          accept=".pdf"
          style={{ display: 'none' }}
          onChange={onFileChange}
        />

        <div style={{ marginBottom: 12 }}>
          <button type="button" onClick={onChooseClick} style={neonStyle}>
            পিডিএফ ফাইল সিলেক্ট করুন 📂
          </button>
          {fileName ? (
            <div style={{ marginTop: 8, color: '#00ffea' }}>{fileName}</div>
          ) : (
            <div style={{ marginTop: 8, color: '#999' }}>কোনো ফাইল নির্বাচিত নেই</div>
          )}
        </div>

        <div>
          <button
            type="submit"
            style={{ ...neonStyle, width: '100%' }}
            disabled={loading}
          >
            কুইজ জেনারেট করুন ⚡
          </button>
        </div>
      </form>

      <div style={{ marginTop: 18 }}>
        {loading && (
          <div style={{ color: '#ffd500' }}>পিডিএফ থেকে বাংলা এমসিকিউ তৈরি হচ্ছে... একটু অপেক্ষা করুন ⚡</div>
        )}

        {!loading && quiz.length > 0 && (
          <div>
            <h3 style={{ color: '#fff' }}>জেনারেটেড কুইজ</h3>
            <ol>
              {quiz.map((q, i) => (
                <li key={i} style={{ marginBottom: 12, color: '#e6e6e6' }}>
                  <div style={{ fontWeight: 700 }}>{q.question}</div>
                  <ul>
                    {q.options
                      ? q.options.map((opt: string, idx: number) => (
                          <li key={idx}>{opt}</li>
                        ))
                      : (
                        <>
                          <li>{q.optionA}</li>
                          <li>{q.optionB}</li>
                          <li>{q.optionC}</li>
                          <li>{q.optionD}</li>
                        </>
                      )}
                  </ul>
                </li>
              ))}
            </ol>
          </div>
        )}
      </div>
    </div>
  )
}

export default UploadDashboard
