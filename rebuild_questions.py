#!/usr/bin/env python3
import json
import pdfplumber
import re

print("📖 Extracting questions from KLCP_Fragenkatalog.pdf...")

# Extract questions from catalog
questions_list = []
with pdfplumber.open('KLCP_Fragenkatalog.pdf') as pdf:
    full_text = ''
    for page in pdf.pages:
        full_text += page.extract_text() + '\n\n'
    
    # Split by "Question" blocks
    blocks = re.split(r'\bQuestion\s+', full_text, flags=re.IGNORECASE)
    
    for block in blocks[1:]:  # Skip first empty part
        lines = [l.strip() for l in block.split('\n') if l.strip()]
        if len(lines) < 5:
            continue
            
        # Extract English question (first non-empty line)
        question_en = lines[0]
        
        # Extract options A-D (English)
        options_en = []
        for line in lines[1:]:
            if line.startswith(('A)', 'B)', 'C)', 'D)')):
                options_en.append(line)
            if len(options_en) == 4:
                break
        
        # Extract German question (after "Frage")
        question_de = ""
        options_de = []
        frage_idx = -1
        for i, line in enumerate(lines):
            if line.lower().startswith('frage'):
                frage_idx = i
                break
        
        if frage_idx >= 0 and frage_idx + 1 < len(lines):
            question_de = lines[frage_idx + 1]
            
            # Get German options
            for line in lines[frage_idx + 2:]:
                if line.startswith(('A)', 'B)', 'C)', 'D)')):
                    options_de.append(line)
                if len(options_de) == 4:
                    break
        
        if len(options_en) == 4:
            questions_list.append({
                'question_en': question_en,
                'question_de': question_de,
                'options_en': options_en,
                'options_de': options_de
            })

print(f"✅ Extracted {len(questions_list)} questions")

print("\n📖 Extracting answers from KLCP_Loesungen.pdf...")

# Extract answers from solutions
answers_list = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    full_text = ''
    for page in pdf.pages:
        full_text += page.extract_text() + '\n\n'
    
    # Split by "Question" blocks
    blocks = re.split(r'\bQuestion\s+', full_text, flags=re.IGNORECASE)
    
    for block in blocks[1:]:
        # Find first "Correct Answer: X"
        match = re.search(r'Correct\s+Answer:\s+([A-D])', block, re.IGNORECASE)
        if match:
            answers_list.append(match.group(1).upper())

print(f"✅ Extracted {len(answers_list)} answers")

# Combine
if len(questions_list) != len(answers_list):
    print(f"⚠️  Mismatch: {len(questions_list)} questions vs {len(answers_list)} answers")
    min_len = min(len(questions_list), len(answers_list))
else:
    min_len = len(questions_list)

# Build final JSON
final_questions = []
for i in range(min_len):
    q = questions_list[i]
    final_questions.append({
        'id': i + 1,
        'question_en': q['question_en'],
        'question_de': q['question_de'],
        'options_en': q['options_en'],
        'options_de': q['options_de'],
        'correct': answers_list[i]
    })

# Load old JSON for comparison
with open('app/src/main/assets/questions.json', 'r') as f:
    old_questions = json.load(f)

print(f"\n🔍 Comparing with existing JSON...")
errors = []
for i in range(min(len(old_questions), len(final_questions))):
    old_q = old_questions[i]
    new_q = final_questions[i]
    
    if old_q['correct'] != new_q['correct']:
        errors.append(i + 1)
        print(f"\n❌ Question {i+1} has wrong answer!")
        print(f"   Old: {old_q['correct']} -> {old_q['options_en'][ord(old_q['correct'])-ord('A')]}")
        print(f"   New: {new_q['correct']} -> {new_q['options_en'][ord(new_q['correct'])-ord('A')]}")
        print(f"   Q: {new_q['question_en'][:70]}...")

if errors:
    print(f"\n⚠️  Found {len(errors)} errors: {errors[:20]}")
    
    # Save corrected version
    with open('app/src/main/assets/questions.json', 'w', encoding='utf-8') as f:
        json.dump(final_questions, f, indent=2, ensure_ascii=False)
    
    print(f"\n✅ Saved corrected questions.json with {len(final_questions)} questions")
else:
    print(f"\n✅ All answers are correct!")

# Show first 10 for verification
print(f"\n📋 First 10 questions:")
for i in range(min(10, len(final_questions))):
    q = final_questions[i]
    print(f"{i+1}. {q['question_en'][:50]}... -> {q['correct']}")

