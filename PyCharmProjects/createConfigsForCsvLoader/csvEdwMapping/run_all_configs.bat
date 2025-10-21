@echo off
setlocal

:: Путь к JAR файлу
set "JAR_FILE=excelLoader-release.jar"

:: Путь к файлу логов
set "LOG_CONFIG=log4j.properties"

:: Директория с конфигурационными файлами
set "CONFIG_DIR=table_structures"

:: Пробегаем по всем конфигурационным файлам в директории
for %%F in (%CONFIG_DIR%\*_config.properties) do (
    echo Запуск для конфига: %%F
    
    :: Запуск Java с конкретным конфигурационным файлом
    java -jar "%JAR_FILE%" "%%F" "%LOG_CONFIG%"
    
    :: Проверка на успешное выполнение команды
    if %errorlevel% equ 0 (
        echo Успешно завершено для %%F
    ) else (
        echo Ошибка при выполнении %%F
    )
)

endlocal
pause
