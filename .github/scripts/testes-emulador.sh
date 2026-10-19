#!/usr/bin/env bash
# Instala o app e os testes no emulador, roda e guarda resultados + screenshots em ci-out/.
set -u
API="${1:-?}"
PACOTE=com.projeto.livrariadigitaleclb
mkdir -p ci-out

# Deixa o sistema terminar de iniciar e esconde avisos do sistema
# ("X não está respondendo"), que no emulador roubam o foco das telas testadas.
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put secure show_first_crash_dialog_dev_option 0 || true
sleep 45
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true
adb shell input keyevent 82 || true

adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c || true

adb shell am instrument -w -r "${PACOTE}.test/androidx.test.runner.AndroidJUnitRunner" > ci-out/instrumentacao.txt 2>&1
cat ci-out/instrumentacao.txt | tail -60

adb pull "/sdcard/Android/data/${PACOTE}/files/screenshots" ci-out/screenshots || true
adb logcat -d -v time > ci-out/logcat.txt || true
grep -E "FATAL|AndroidRuntime|Exception" ci-out/logcat.txt | head -200 > ci-out/erros-logcat.txt || true
echo "API ${API}" > ci-out/api.txt

if grep -q "FAILURES!!!" ci-out/instrumentacao.txt || ! grep -q "OK (" ci-out/instrumentacao.txt; then
  echo "Testes no emulador falharam"
  exit 1
fi
