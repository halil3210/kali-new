#!/usr/bin/env python3
"""
Manually verify and correct ALL question answers by reading each question
and determining the correct answer based on Linux knowledge.
"""
import json

# Load questions
with open('app/src/main/assets/questions.json', 'r', encoding='utf-8') as f:
    questions = json.load(f)

print(f"📋 Analyzing {len(questions)} questions...\n")

# Manual corrections based on reading each question
corrections = {
    # Q1: What does the Linux kernel do? -> D (manages everything)
    1: 'D',
    
    # Q2: kernel space vs user space? -> C (ring 0 vs applications)
    2: 'C',
    
    # Q3: How identify block/char device? -> C (ls -l shows 'b' or 'c')
    3: 'C',
    
    # Q4: Where are device files? -> D (/dev/ directory)
    4: 'D',
    
    # Q5: What is PID? -> C (Process Identifier)
    5: 'C',
    
    # Q6: How does Linux handle multi-tasking? -> C (time slicing)
    6: 'C',
    
    # Q7: Command to mount filesystem? -> C (mount)
    7: 'C',
    
    # Q8: Common Linux filesystems? -> D (ext2, ext3, ext4)
    8: 'D',
    
    # Q9: What is VFAT? -> C (DOS/Windows filesystem)
    9: 'C',
    
    # Q10: What is NFS? -> D (Network File System)
    10: 'D',
    
    # Q11: Purpose of /proc/? -> B (virtual filesystem for hardware/processes)
    11: 'B',
}

# Apply corrections
corrected_count = 0
for q_id, correct_ans in corrections.items():
    if q_id <= len(questions):
        q = questions[q_id - 1]
        old_ans = q['correct']
        
        if old_ans != correct_ans:
            q['correct'] = correct_ans
            corrected_count += 1
            
            print(f"✏️  Q{q_id}: {old_ans} → {correct_ans}")
            print(f"    {q['question_en'][:70]}...")
            print(f"    ✅ {q['options_en'][ord(correct_ans)-ord('A')][:70]}...")
            print()

print(f"\n{'='*80}")
print(f"✅ Corrected {corrected_count} answers out of {len(corrections)} reviewed")

# Save
with open('app/src/main/assets/questions.json', 'w', encoding='utf-8') as f:
    json.dump(questions, f, indent=2, ensure_ascii=False)

print(f"💾 Saved to questions.json")
print(f"\n📝 Continue reviewing remaining questions manually...")

