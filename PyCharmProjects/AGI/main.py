import speech_recognition

sr = speech_recognition.Recognizer()
sr.pause_threshold = 0.5  # создаем паузу, после которой ассистент примет нашу команду


def listen_comand():

    try:
        with speech_recognition.Microphone() as mic:
            sr.adjust_for_ambient_noise(source=mic, duration=0.5)
            audio = sr.listen(source=mic)
            query = sr.recognize_google(audio_data=audio, language='ru-RU').lower()
        return query

    except speech_recognition.UnknownValueError:
        return 'Я не понял что ты сказал'

commands_dict = {
    'commads': {
        'search_for_information_on_google': ['искать', 'гугл', 'найди', 'найти']
    }
}


def main():
    query = listen_comand()


if __name__ == '__main__':
    main()
