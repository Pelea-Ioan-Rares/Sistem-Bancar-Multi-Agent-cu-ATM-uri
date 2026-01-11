from typing import List
from pydantic import BaseModel, Field
from fastapi import FastAPI
import os, sys

# ---------------- Pydantic Ad model ----------------
class AdMessage(BaseModel):
    title: str = Field(min_length=5, max_length=100)
    description: str = Field(min_length=10, max_length=300)
    min_purchase: float
    reward: str

# ---------------- Config Ollama ----------------
OLLAMA_MODEL_NAME = os.getenv("OLLAMA_MODEL", "qwen2.5:1.5b-instruct")
OLLAMA_BASE = os.getenv("OLLAMA_BASE", "http://localhost:11434")  # fără /v1
USE_OLLAMA = os.getenv("USE_OLLAMA", "1").lower() in ["1", "true", "yes"]

agent = None

if USE_OLLAMA:
    try:
        from pydantic_ai.models.openai import OpenAIChatModel
        from pydantic_ai.providers.ollama import OllamaProvider
        from pydantic_ai import Agent

        MODEL = OpenAIChatModel(
            model_name=OLLAMA_MODEL_NAME,
            provider=OllamaProvider(base_url=f"{OLLAMA_BASE}/v1"),
        )

        agent = Agent(
            MODEL,
            output_type=AdMessage,
            system_prompt=(
                "Generate a short, catchy ATM advertisement in English. "
                "Output must be valid JSON strictly matching AdMessage: "
                '{"title": "...", "description": "...", "min_purchase": ..., "reward": "..."}'
            ),
            retries=2,
            output_retries=3,
        )

        print(f"OLLAMA status: True (model {OLLAMA_MODEL_NAME})")
    except Exception as e:
        print(f"Ollama init failed: {e}")
        USE_OLLAMA = False
        agent = None
else:
    print(f"OLLAMA status: False (using fallback)")

# ---------------- Fallback ad ----------------
FALLBACK_AD = AdMessage(
    title="ATM Promotion!",
    description="Make purchases of at least 500 lei and you could win a vacation in Dubai!",
    min_purchase=500,
    reward="vacation in Dubai"
)

# ---------------- FastAPI app ----------------
app = FastAPI(title="ATM Marketing Agent")

@app.get("/ad", response_model=AdMessage)
async def get_ad():
    """Return AI-generated ad or fallback."""
    if not USE_OLLAMA or agent is None:
        return FALLBACK_AD

    prompt = (
        "Generate a short, catchy ATM advertisement in English. "
        "Include: title (5-100 chars), description (10-300 chars), min_purchase, reward. "
        "Return STRICTLY valid JSON matching AdMessage."
    )

    try:
        # Folosim agent.run(prompt) simplu
        result = await agent.run(prompt)
        if isinstance(result.output, dict):
            return AdMessage(**result.output)
        elif isinstance(result.output, AdMessage):
            return result.output
        else:
            return FALLBACK_AD
    except Exception as e:
        print(f"Ollama failed: {e}")
        return FALLBACK_AD

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "use_ollama": USE_OLLAMA,
        "provider": "ollama" if USE_OLLAMA else "fallback",
        "model_name": OLLAMA_MODEL_NAME if USE_OLLAMA else "None",
        "python_version": sys.version.split()[0],
        "pid": os.getpid(),
    }
