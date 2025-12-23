#!/usr/bin/env python3
import json
import pdfplumber
import re

print("🔍 Extracting all questions from KLCP_Fragenkatalog.pdf...")

# Extract questions from catalog
questions_data = []
with pdfplumber.open('KLCP_Fragenkatalog.pdf') as pdf:
    full_text = ''
    for page in pdf.pages:
        text = page.extract_text()
        if text:
            full_text += text + '\n\n===PAGE_BREAK===\n\n'
    
    # Split by "Question " (with space)
    blocks = re.split(r'\n\s*Question\s+\n', full_text, flags=re.IGNORECASE)
    
    for block_idx, block in enumerate(blocks[1:], start=1):  # Skip first empty
        lines = [l.strip() for l in block.split('\n') if l.strip() and l.strip() != '===PAGE_BREAK===']
        
        if len(lines) < 6:
            continue
        
        # First line should be English question
        question_en = lines[0]
        
        # Next 4 lines should be English options A-D
        options_en = []
        idx = 1
        while idx < len(lines) and len(options_en) < 4:
            line = lines[idx]
            if re.match(r'^[A-D]\)', line):
                options_en.append(line)
            idx += 1
        
        if len(options_en) != 4:
            print(f"⚠️  Question {block_idx}: Only found {len(options_en)} English options")
            continue
        
        # Find "Frage" for German section
        question_de = ""
        options_de = []
        
        for i in range(idx, len(lines)):
            if lines[i].lower().startswith('frage'):
                # Next line is German question
                if i + 1 < len(lines):
                    question_de = lines[i + 1]
                    
                    # Get German options
                    for j in range(i + 2, len(lines)):
                        if re.match(r'^[A-D]\)', lines[j]):
                            options_de.append(lines[j])
                            if len(options_de) == 4:
                                break
                break
        
        if len(options_de) != 4:
            # Try to copy from English if German not found
            options_de = options_en.copy()
            if not question_de:
                question_de = question_en
        
        questions_data.append({
            'question_en': question_en,
            'question_de': question_de,
            'options_en': options_en,
            'options_de': options_de
        })

print(f"✅ Extracted {len(questions_data)} questions")

print("\n🔍 Extracting all answers from KLCP_Loesungen.pdf...")

# Extract answers from solutions
answers_list = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    full_text = ''
    for page in pdf.pages:
        text = page.extract_text()
        if text:
            full_text += text + '\n\n===PAGE_BREAK===\n\n'
    
    # Split by "Question " blocks
    blocks = re.split(r'\n\s*Question\s+\n', full_text, flags=re.IGNORECASE)
    
    for block in blocks[1:]:  # Skip first empty
        # Find FIRST "Correct Answer: X" in this block
        match = re.search(r'Correct\s+Answer:\s+([A-D])', block, re.IGNORECASE)
        if match:
            answers_list.append(match.group(1).upper())

print(f"✅ Extracted {len(answers_list)} answers")

# Combine questions and answers
min_count = min(len(questions_data), len(answers_list))
print(f"\n✨ Creating {min_count} complete questions...")

complete_questions = []
for i in range(min_count):
    complete_questions.append({
        'id': i + 1,
        'question_en': questions_data[i]['question_en'],
        'question_de': questions_data[i]['question_de'],
        'options_en': questions_data[i]['options_en'],
        'options_de': questions_data[i]['options_de'],
        'correct': answers_list[i]
    })

# Save to file
output_file = 'app/src/main/assets/questions.json'
with open(output_file, 'w', encoding='utf-8') as f:
    json.dump(complete_questions, f, indent=2, ensure_ascii=False)

print(f"\n✅ Saved {len(complete_questions)} questions to {output_file}")

# Show first 10 questions as verification
print("\n📋 First 10 questions for verification:")
print("="*80)
for i in range(min(10, len(complete_questions))):
    q = complete_questions[i]
    print(f"\nQ{q['id']}: {q['question_en'][:60]}...")
    print(f"Answer: {q['correct']}")
    correct_option = q['options_en'][ord(q['correct']) - ord('A')]
    print(f"  ✅ {correct_option}")

print("\n" + "="*80)
print(f"✅ DONE! {len(complete_questions)} questions with correct answers saved!")

