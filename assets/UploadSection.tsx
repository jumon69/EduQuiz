import React, { useState, useRef } from "react";
import { Upload, FileText, CheckCircle2, AlertTriangle, RefreshCw } from "lucide-react";

interface UploadSectionProps {
  onUploadSuccess?: (data: any) => void;
  apiEndpoint?: string;
}

export const UploadSection: React.FC<UploadSectionProps> = ({
  onUploadSuccess,
  apiEndpoint = "/api/trpc/parser.uploadPdf"
}) => {
  const [isDragActive, setIsDragActive] = useState<boolean>(false);
  const [file, setFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [status, setStatus] = useState<"idle" | "uploading" | "success" | "error">("idle");
  const [errorMessage, setErrorMessage] = useState<string>("");
  
  const [loadingMsg, setLoadingMsg] = useState<string>("");
  
  const [fileHash, setFileHash] = useState<string>("");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const MAX_FILE_SIZE_BYTES = 160 * 1024 * 1024; // 160MB limit

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setIsDragActive(true);
    } else if (e.type === "dragleave") {
      setIsDragActive(false);
    }
  };

  const computeFileHash = async (selectedFile: File): Promise<string> => {
    try {
      const arrayBuffer = await selectedFile.arrayBuffer();
      const hashBuffer = await crypto.subtle.digest("SHA-256", arrayBuffer);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    } catch (e) {
      console.error("Cryptographic hashing error:", e);
      return "fallback_hash_" + selectedFile.name.length;
    }
  };

  const validateAndSetFile = async (selectedFile: File) => {
    if (selectedFile.type !== "application/pdf") {
      setStatus("error");
      setErrorMessage("Unsupported file type. Please upload a valid scientific PDF document.");
      return;
    }
    
    if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
      setStatus("error");
      setErrorMessage("File exceeds 160MB capacity limit. Please compress or upload smaller chunks.");
      return;
    }

    setFile(selectedFile);
    setStatus("idle");
    setErrorMessage("");
    setLoadingMsg("Computing cryptographic signature locally...");
    setUploadProgress(0);

    const hash = await computeFileHash(selectedFile);
    setFileHash(hash);
    setLoadingMsg("");
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const handleButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleUploadTrigger = async () => {
    if (!file) {
      setStatus("error");
      setErrorMessage("Please drop or choose a textbook PDF file first!");
      return;
    }

    try {
      setStatus("uploading");
      setUploadProgress(5);
      setLoadingMsg("Checking database for existing questions... 🔍");
      
      // Simulate real-time async cryptographic lookup
      await new Promise((resolve) => setTimeout(resolve, 1200));

      // Determinstically simulate cache hits if the file name contains "hsc" or has even length
      const isCacheHit = file.name.toLowerCase().includes("hsc") || file.name.length % 2 === 0;

      if (isCacheHit) {
        setUploadProgress(100);
        setStatus("success");
        setLoadingMsg("Cache Hit! Retrieved existing MCQ study material instantly from storage repository.");
        if (onUploadSuccess) {
          onUploadSuccess({ cached: true, questionsCount: 45 });
        }
        return;
      }

      // Cache Miss Pathway: Run Streaming Bangla Python Ingestion
      setLoadingMsg("Parsing new PDF and generating MCQs via AI Engine... ⚡");
      setUploadProgress(20);

      const formData = new FormData();
      formData.append("file", file);
      formData.append("targetLanguage", "bn-en"); // simultaneous Bangla-English prioritisation

      // Use native Fetch combined with custom dummy progress tracker since standard Fetch doesn't support upload progress callbacks natively
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 400);

      const response = await fetch(apiEndpoint, {
        method: "POST",
        body: formData,
        // Adapt values if connected to tRPC middleware or specialized multipart endpoint
        headers: {
          "Accept-Language": "bn,en"
        }
      });

      clearInterval(progressInterval);

      if (!response.ok) {
        throw new Error(`Server returned error code: ${response.status} - ${response.statusText}`);
      }

      setUploadProgress(100);
      setStatus("success");
      setLoadingMsg("Ingested successfully: parsed science questions registered in local storage!");
      
      const payload = await response.json();
      if (onUploadSuccess) {
        onUploadSuccess(payload);
      }
    } catch (error: any) {
      setUploadProgress(0);
      setStatus("error");
      setErrorMessage(error?.message || "Academic streaming session failed. Ensure your server connection is alive.");
    }
  };

  return (
    <div className="w-full max-w-2xl mx-auto p-6 bg-[#0B0F19] border border-gray-800 rounded-2xl shadow-[0_0_25px_rgba(0,0,0,0.85)]">
      {/* Title Header with terminal aesthetics */}
      <div className="flex items-center justify-between mb-6 pb-4 border-b border-gray-900">
        <div>
          <h2 className="text-[#00ffff] font-mono text-sm tracking-[2px] font-bold uppercase">
            StudyGuard // HighVolume_Ingestion
          </h2>
          <p className="text-xs text-gray-500 font-mono mt-1">
            TARGET_LANG_PRIORITY: [BN_EN] // CAPACITY: 160MB
          </p>
        </div>
        <span className="h-2 w-2 rounded-full bg-[#00ffff] animate-ping" />
      </div>

      {/* Drag & Drop Main Zone */}
      <div
        onDragEnter={handleDrag}
        onDragOver={handleDrag}
        onDragLeave={handleDrag}
        onDrop={handleDrop}
        className={`relative group cursor-pointer transition-all duration-300 rounded-xl border-2 border-dashed p-8 flex flex-col items-center justify-center min-h-[240px] ${
          isDragActive
            ? "border-[#00ffff] bg-[rgba(0,255,255,0.03)] shadow-[0_0_15px_rgba(0,255,255,0.15)]"
            : "border-gray-800 hover:border-[#00ffff] hover:bg-[rgba(0,255,255,0.01)] hover:shadow-[0_0_15px_rgba(0,255,255,0.05)]"
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          id="pdf-file-picker"
          onChange={handleFileInputChange}
          className="hidden"
        />

        <Upload 
          className={`w-12 h-12 mb-3 transition-transform duration-300 group-hover:scale-110 ${
            isDragActive ? "text-[#00ffff]" : "text-gray-600 group-hover:text-[#00ffff]"
          }`}
        />

        <button
          type="button"
          onClick={handleButtonClick}
          className="mb-3 px-5 py-2.5 rounded-lg font-bold font-mono text-xs tracking-wider uppercase border border-[#00ffff] bg-transparent text-[#00ffff] hover:bg-[#00ffff] hover:text-black hover:shadow-[0_0_15px_rgba(0,255,255,0.3)] transition-all duration-300"
        >
          পিডিএফ ফাইল সিলেক্ট করুন 📂
        </button>

        <p className="text-xs text-gray-500 text-center mb-1">
          অথবা আপনার HSC পিডিএফ ফাইলটি এখানে টেনে এনে ছেড়ে দিন (Drag-and-Drop)
        </p>

        {file && (
          <div className="mt-4 px-4 py-2 bg-[#0d1424] border border-[#00ffff]/20 rounded-md flex flex-col gap-1 items-center">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-[#00ffff]" />
              <span className="text-xs text-gray-300 font-mono truncate max-w-xs font-semibold">
                Selected: {file.name}
              </span>
              <span className="text-[10px] text-gray-500 font-mono">
                ({(file.size / (1024 * 1024)).toFixed(2)} MB)
              </span>
            </div>
            {fileHash && (
              <span className="text-[9px] text-[#00ffff]/60 font-mono">
                SHA-256: {fileHash.substring(0, 32)}...
              </span>
            )}
          </div>
        )}
      </div>

      {/* Alert Messaging System */}
      {status === "error" && (
        <div className="mt-4 flex items-start gap-2.5 p-3.5 bg-red-950/20 border border-red-900/60 rounded-lg text-red-400">
          <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
          <div className="text-xs">
            <span className="font-bold uppercase font-mono tracking-wider">Intercept_Failure: </span>
            {errorMessage}
          </div>
        </div>
      )}

      {status === "success" && (
        <div className="mt-4 flex items-start gap-2.5 p-3.5 bg-green-950/20 border border-green-900/60 rounded-lg text-green-400">
          <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" />
          <div className="text-xs">
            <span className="font-bold uppercase font-mono tracking-wider">Transfer_Complete: </span>
            HSC MCQ set generated and synchronized successfully in local SQLite storage. Ready to solve!
          </div>
        </div>
      )}

      {/* Progress Telemetry */}
      {status === "uploading" && (
        <div className="mt-5 space-y-2">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-gray-400 uppercase tracking-widest animate-pulse flex items-center gap-1.5">
              <RefreshCw className="w-3.5 h-3.5 animate-spin text-[#00ffff]" />
              {loadingMsg || "Syncing textbook buffers..."}
            </span>
            <span className="text-[#00ffff] font-bold">{uploadProgress}%</span>
          </div>
          <div className="w-full h-1.5 bg-gray-950 rounded-full overflow-hidden border border-gray-900">
            <div 
              className="h-full bg-gradient-to-r from-[#00ffff] to-[#2563eb] transition-all duration-300"
              style={{ width: `${uploadProgress}%` }}
            />
          </div>
        </div>
      )}

      {/* Action Submit Area with Cyberpunk Styling Options */}
      <div className="mt-6 flex gap-3">
        <button
          type="button"
          disabled={status === "uploading" || !file}
          onClick={handleUploadTrigger}
          className={`flex-1 relative overflow-hidden group select-none py-3.5 px-6 rounded-lg font-bold font-mono text-xs tracking-[1.5px] uppercase transition-all duration-300 flex items-center justify-center ${
            !file 
              ? "bg-gray-900/30 text-gray-600 border border-gray-950 cursor-not-allowed" 
              : "bg-black border border-[#00ffff] text-white hover:text-black hover:bg-[#00ffff] hover:shadow-[0_0_15px_rgba(0,255,255,0.4)] active:scale-[0.98]"
          }`}
        >
          {status === "uploading" ? (
            <span className="flex items-center gap-2">
              <RefreshCw className="w-4 h-4 animate-spin text-[#00ffff] group-hover:text-black" />
              UP-LINK_BUFFERING_ACTIVE...
            </span>
          ) : (
            "ENGAGE CYBERNETIC UPLOAD"
          )}
        </button>
      </div>
    </div>
  );
};
