import os

search_dir = "src/main/java/com/example/demo"
old_import = "import com.example.demo.infra.event.shared.event."
new_import = "import com.example.demo.application.shared.event."

for root, dirs, files in os.walk(search_dir):
    for file in files:
        if file.endswith(".java"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            if old_import in content:
                content = content.replace(old_import, new_import)
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {filepath}")
