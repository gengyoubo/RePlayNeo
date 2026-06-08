#!/usr/bin/env python3
import json
import os

# Whether to add entries present in the original but not yet in the translation
# Can be very useful to complete translations but makes it difficult to tell which entries have yet to be translated.
# May be set to True, False or a string which will be prepended to yet-to-be-translated lines
include_missing: str | bool = False

# Whether to keep entries which are identical between translation and original
# Disabling can be useful to clean up partially translated files, but it will also remove entries where the translation
# just happens to match, so keep those by default.
include_originals = True

template_file_name = 'en_us.json'

with open(template_file_name, encoding='utf-8') as f:
    template_entries = json.load(f)

for file_name in os.listdir('.'):
    if not file_name.endswith('.json'):
        continue

    with open(file_name, 'r', encoding='utf-8') as f:
        entries = json.load(f)

    sorted_entries = {}
    for key, default_value in template_entries.items():
        value = entries[key] if key in entries else default_value
        has_translation = key in entries and (value != default_value or include_originals)

        if not has_translation and include_missing is False:
            continue

        if has_translation or include_missing is True:
            sorted_entries[key] = value
        else:
            sorted_entries[include_missing + key] = value

    with open(file_name, 'w', encoding='utf-8') as f:
        json.dump(sorted_entries, f, ensure_ascii=False, indent=2)
        f.write('\n')
