#!/usr/bin/env python3
import json
import pdfplumber
import re

print("📖 Loading existing questions.json...")
with open('app/src/main/assets/questions.json', 'r', encoding='utf-8') as f:
    questions = json.load(f)

print(f"✅ Loaded {len(questions)} questions")

print("\n🔍 Extracting correct answers from KLCP_Loesungen.pdf...")

# Extract answers from solutions PDF
answers_list = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    full_text = ''
    for page in pdf.pages:
        text = page.extract_text()
        if text:
            full_text += text + '\n\n'
    
    # Split by "Question" keyword
    blocks = re.split(r'\bQuestion\s+\n', full_text, flags=re.IGNORECASE | re.MULTILINE)
    
    print(f"Found {len(blocks)} blocks in PDF")
    
    for block in blocks[1:]:  # Skip header
        # Find FIRST "Correct Answer: X" in this block
        match = re.search(r'Correct\s+Answer:\s+([A-D])', block, re.IGNORECASE)
        if match:
            answers_list.append(match.group(1).upper())

print(f"✅ Extracted {len(answers_list)} answers from PDF")

# Update questions with correct answers
if len(answers_list) < len(questions):
    print(f"⚠️  Warning: Only {len(answers_list)} answers for {len(questions)} questions")
    print(f"   Will update first {len(answers_list)} questions")

errors_found = []
for i in range(min(len(questions), len(answers_list))):
    old_answer = questions[i]['correct']
    new_answer = answers_list[i]
    
    if old_answer != new_answer:
        errors_found.append({
            'id': questions[i]['id'],
            'old': old_answer,
            'new': new_answer,
            'question': questions[i]['question_en'][:70]
        })
        questions[i]['correct'] = new_answer

# Show errors
if errors_found:
    print(f"\n❌ Found {len(errors_found)} incorrect answers:")
    print("="*80)
    for err in errors_found[:20]:  # Show first 20
        print(f"\nQ{err['id']}: {err['question']}...")
        print(f"  OLD: {err['old']} -> NEW: {err['new']}")
    
    if len(errors_found) > 20:
        print(f"\n... and {len(errors_found) - 20} more errors")
else:
    print("\n✅ All answers were already correct!")

# Save updated JSON
with open('app/src/main/assets/questions.json', 'w', encoding='utf-8') as f:
    json.dump(questions, f, indent=2, ensure_ascii=False)

print(f"\n✅ Saved corrected questions.json with {len(questions)} questions")
print(f"   {len(errors_found)} answers were corrected")

# Verify first 10
print("\n📋 First 10 questions (verification):")
print("="*80)
for i in range(min(10, len(questions))):
    q = questions[i]
    correct_opt = q['options_en'][ord(q['correct']) - ord('A')]
    print(f"Q{q['id']}: Answer={q['correct']} -> {correct_opt[:50]}...")

