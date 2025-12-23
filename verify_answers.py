#!/usr/bin/env python3
import json
import pdfplumber
import re

# Load current questions
with open('app/src/main/assets/questions.json', 'r', encoding='utf-8') as f:
    questions = json.load(f)

# Extract correct answers from PDF (sequential, no question numbers)
correct_answers_list = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    for page in pdf.pages:
        text = page.extract_text()
        if not text:
            continue
        
        # Find all "Correct Answer: X" patterns
        matches = re.findall(r'Correct\s+Answer:\s+([A-D])', text, re.IGNORECASE)
        correct_answers_list.extend([m.upper() for m in matches])

print(f"Found {len(correct_answers_list)} correct answers from PDF")
print(f"JSON has {len(questions)} questions\n")

# Map to question IDs (1-indexed)
pdf_answers = {i+1: ans for i, ans in enumerate(correct_answers_list)}

# Compare with JSON
errors = []
for q in questions:
    q_id = q['id']
    json_answer = q['correct']
    
    if q_id in pdf_answers:
        pdf_answer = pdf_answers[q_id]
        if json_answer != pdf_answer:
            errors.append({
                'id': q_id,
                'question': q['question_en'],
                'json_answer': json_answer,
                'pdf_answer': pdf_answer
            })
            print(f"❌ Question {q_id}: JSON has '{json_answer}' but PDF says '{pdf_answer}'")
            print(f"   Q: {q['question_en'][:80]}...")
            print(f"   Options: {q['options_en']}")
            print(f"   JSON Answer: {q['options_en'][ord(json_answer)-ord('A')]}")
            print(f"   PDF Answer:  {q['options_en'][ord(pdf_answer)-ord('A')]}")
            print()

if not errors:
    print("✅ All answers match!")
else:
    print(f"\n⚠️  {len(errors)} errors found!")
    
    # Show first 10 PDF answers for verification
    print("\nFirst 10 PDF answers:")
    for i in range(1, min(11, len(pdf_answers)+1)):
        print(f"  Q{i}: {pdf_answers.get(i, '?')}")
    
    # Generate corrected JSON
    print("\n🔧 Generating corrected questions.json...")
    for q in questions:
        if q['id'] in pdf_answers:
            q['correct'] = pdf_answers[q['id']]
    
    with open('app/src/main/assets/questions_corrected.json', 'w', encoding='utf-8') as f:
        json.dump(questions, f, indent=2, ensure_ascii=False)
    
    print("✅ Corrected file saved as questions_corrected.json")
    print(f"\nTo apply: mv app/src/main/assets/questions_corrected.json app/src/main/assets/questions.json")
