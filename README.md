# Sistem Bancar Multi-Agent cu ATM-uri (JADE + AI)

Acest proiect implementează un sistem bancar distribuit folosind **JADE (Java Agent DEvelopment Framework)**, cu:
- agenți Bank, ATM și User
- interfață grafică Java Swing
- agent AI (Ollama) pentru generarea de reclame afișate la deschiderea unui ATM

---

## Cerințe

### Software necesar
- **Java JDK 8+**
- **JADE 4.5+**
- **Python 3.10+**
- **Ollama** 

---

## Ollama 

Instalați conform instrucțiunilor de la adresa https://ollama.com.

După instalare, serviciul expune API la http://localhost:11434.
Comenzi de bază după pornirea aplicației:

# Verificare că Ollama funcționează
ollama --version 

# Descărcare model (ex.: Qwen 2.5 1.5B Instruct)
ollama pull qwen2.5:1.5b-instruct

# Rulare interactivă în terminal
ollama run qwen2.5:1.5b-instruct

# Dacă modelul e pornit și se dorește afișarea opțiunilor
>>> /?

# Părăsirea sesiunii curente
>>> /bye

# Afișarea informațiilor modelului din sesiunea curentă
>>> /show

# Listă modele disponibile local
ollama list

# Verificarea modelului încărcat în mod curent în memorie (folosiți un alt terminal!)
ollama ps

# Dacă se dorește generarea raportului de performanță după închiderea sesiunii Ollama
ollama run qwen2.5:1.5b-instruct --verbose

---

## Activarea agentului AI

# Navigati in folderul AI din interiorul proiectuluiȘ

# Activarea mediului virtual
venv\Scripts\activate

# Instalarea dependințe
pip install -r requirements.txt

# Lansarea serviciului Pydantic (utilizarea portului 8001 deoarece portul 8000 este deseori utilizat deja)
uvicorn marketing_agent:app --port 8001

---

## Lansare în execuție agenți

Din folderul principal rulati:
java -cp "bin;bin\jade\jade.jar" banking.MainContainer

Utilizare program:
    Utilizator se poate conecta la banca centrala pentru a-si deschide un cont, sau la un ATM pentru a depozita sau adauga fonduri in contul acestuia.
