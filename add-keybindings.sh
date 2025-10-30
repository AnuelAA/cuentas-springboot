#!/bin/bash
# Script para agregar atajos al archivo keybindings.json de Cursor

KEYBINDINGS_FILE="$HOME/Library/Application Support/Cursor/User/keybindings.json"
BACKUP_FILE="$HOME/Library/Application Support/Cursor/User/keybindings.json.backup"

# Crear backup
cp "$KEYBINDINGS_FILE" "$BACKUP_FILE"
echo "✓ Backup creado en: $BACKUP_FILE"

# Leer el contenido actual
CURRENT_CONTENT=$(cat "$KEYBINDINGS_FILE")

# Preparar el nuevo contenido
NEW_BINDINGS='[
    {
        "key": "cmd+shift+r",
        "command": "workbench.action.tasks.runTask",
        "args": "Compilar y Ejecutar (perfil local)"
    },
    {
        "key": "cmd+shift+b",
        "command": "workbench.action.tasks.runTask",
        "args": "Maven: Compilar (local)"
    }
]'

# Si el archivo ya tiene un array, agregar al array existente
if [[ "$CURRENT_CONTENT" == *"[ "* ]]; then
    # Remover el último ']' y agregar los nuevos bindings
    TEMP_CONTENT=$(echo "$CURRENT_CONTENT" | sed '$s/]$//')
    echo "$TEMP_CONTENT," > /tmp/new_keybindings.json
    echo "$NEW_BINDINGS" | sed 's/\[//' | sed 's/\]//' >> /tmp/new_keybindings.json
    echo "]" >> /tmp/new_keybindings.json
    mv /tmp/new_keybindings.json "$KEYBINDINGS_FILE"
    echo "✓ Atajos agregados al archivo existente"
else
    echo "$NEW_BINDINGS" > "$KEYBINDINGS_FILE"
    echo "✓ Nuevo archivo de atajos creado"
fi

echo ""
echo "Para que los cambios surtan efecto:"
echo "1. Reinicia Cursor, O"
echo "2. Presiona Cmd+Shift+P y ejecuta 'Developer: Reload Window'"
echo ""
echo "Atajos configurados:"
echo "  - Cmd+Shift+R: Compilar y Ejecutar (perfil local)"
echo "  - Cmd+Shift+B: Solo compilar"

