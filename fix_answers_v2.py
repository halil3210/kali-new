#!/usr/bin/env python3
import json
import pdfplumber
import re

print("📖 Loading existing questions.json...")
with open('app/src/main/assets/questions.json', 'r', encoding='utf-8') as f:
    questions = json.load(f)

print(f"✅ Loaded {len(questions)} questions")

print("\n🔍 Extracting correct answers from KLCP_Loesungen.pdf...")

# Extract ALL "Correct Answer: X" occurrences
all_matches = []
with pdfplumber.open('KLCP_Loesungen.pdf') as pdf:
    for page_num, page in enumerate(pdf.pages, 1):
        text = page.extract_text()
        if text:
            # Find all "Correct Answer: X" on this page
            matches = re.findall(r'Correct\s+Answer:\s+([A-D])', text, re.IGNORECASE)
            for match in matches:
                all_matches.append(match.upper())
            
            if matches:
                print(f"  Page {page_num}: Found {len(matches)} answer(s)")

print(f"\n✅ Extracted {len(all_matches)} total 'Correct Answer' entries")

# The PDF has answers in both EN and DE, so we get duplicates
# Take only unique sequential answers (every 2nd occurrence)
answers_list = []
seen_in_sequence = []
for i, ans in enumerate(all_matches):
    if i == 0:
        answers_list.append(ans)
        seen_in_sequence = [ans]
    elif i % 2 == 0:  # Even index - new question
        answers_list.append(ans)
        seen_in_sequence = [ans]
    # Skip odd indices (German duplicates)

print(f"📝 After deduplication: {len(answers_list)} unique answers")

# Update questions with correct answers
if len(answers_list) != len(questions):
    print(f"⚠️  Mismatch: {len(answers_list)} answers vs {len(questions)} questions")
    min_count = min(len(answers_list), len(questions))
    print(f"   Will update first {min_count} questions")
else:
    min_count = len(questions)

errors_found = []
for i in range(min_count):
    old_answer = questions[i]['correct']
    new_answer = answers_list[i]
    
    if old_answer != new_answer:
        q = questions[i]
        old_opt = q['options_en'][ord(old_answer) - ord('A')] if old_answer in 'ABCD' else '?'
        new_opt = q['options_en'][ord(new_answer) - ord('A')] if new_answer in 'ABCD' else '?'
        
        errors_found.append({
            'id': q['id'],
            'old': old_answer,
            'new': new_answer,
            'question': q['question_en']
        })
        
        questions[i]['correct'] = new_answer
        
        print(f"\n❌ Q{q['id']}: {q['question_en'][:60]}...")
        print(f"   OLD: {old_answer}) {old_opt[:50]}...")
        print(f"   NEW: {new_answer}) {new_opt[:50]}...")

# Summary
print(f"\n{'='*80}")
if errors_found:
    print(f"⚠️  Found and corrected {len(errors_found)} wrong answers")
else:
    print("✅ All answers were already correct!")

# Save updated JSON
with open('app/src/main/assets/questions.json', 'w', encoding='utf-8') as f:
    json.dump(questions, f, indent=2, ensure_ascii=False)

print(f"\n✅ Saved corrected questions.json")
print(f"   Total questions: {len(questions)}")
print(f"   Corrections made: {len(errors_found)}")

# Show first 10 for verification
print("\n📋 First 10 questions:")
for i in range(min(10, len(questions))):
    q = questions[i]
    opt = q['options_en'][ord(q['correct']) - ord('A')]
    print(f"  Q{q['id']}: {q['correct']}) {opt[:60]}...")

