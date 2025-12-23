#!/usr/bin/env python3
import json
import pdfplumber
import re

# Extract answers - only one per "Question" block
pdf_answers = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    full_text = ""
    for page in pdf.pages:
        text = page.extract_text()
        if text:
            full_text += text + "\n"
    
    # Split by "Question" and extract first "Correct Answer:" from each block
    blocks = re.split(r'\bQuestion\s+', full_text, flags=re.IGNORECASE)
    
    for block in blocks[1:]:  # Skip first empty block
        # Find first "Correct Answer: X" in this block
        match = re.search(r'Correct\s+Answer:\s+([A-D])', block, re.IGNORECASE)
        if match:
            pdf_answers.append(match.group(1).upper())

print(f"📋 Extracted {len(pdf_answers)} answers from PDF")

# Load current questions
with open('app/src/main/assets/questions.json', 'r', encoding='utf-8') as f:
    questions = json.load(f)

print(f"📋 JSON has {len(questions)} questions\n")

# Compare
errors = []
for i, q in enumerate(questions):
    q_id = q['id']
    json_answer = q['correct']
    
    if i < len(pdf_answers):
        pdf_answer = pdf_answers[i]
        
        if json_answer != pdf_answer:
            errors.append(q_id)
            print(f"❌ Question {q_id}: JSON='{json_answer}' vs PDF='{pdf_answer}'")
            print(f"   {q['question_en'][:70]}...")
            
            # Show the options
            for opt in q['options_en']:
                marker = "👉" if opt.startswith(f"{pdf_answer})") else "  "
                print(f"   {marker} {opt}")
            print()

print(f"\n{'='*60}")
if errors:
    print(f"⚠️  Found {len(errors)} incorrect answers: {errors}")
    print(f"\n🔧 Correcting...")
    
    # Apply corrections
    for i, q in enumerate(questions):
        if i < len(pdf_answers):
            q['correct'] = pdf_answers[i]
    
    # Save corrected version
    with open('app/src/main/assets/questions.json', 'w', encoding='utf-8') as f:
        json.dump(questions, f, indent=2, ensure_ascii=False)
    
    print(f"✅ Corrected {len(errors)} answers in questions.json")
else:
    print("✅ All answers are correct!")

