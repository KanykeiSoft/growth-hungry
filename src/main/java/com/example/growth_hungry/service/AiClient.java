package com.example.growth_hungry.service;

public interface AiClient {
    /**
     * Отправляет текст пользователя в модель и возвращает текст ответа.
     *
     * @param message      пользовательский ввод, обязателен (не null/blank)
     * @param systemPrompt системная инструкция (может быть null → не добавлять)
     * @param model        имя модели (может быть null → использовать дефолт из конфигурации)
     * @return текст ответа модели (может быть пустым; сервис подставит заглушку)
     * @throws IllegalArgumentException при некорректных аргументах (например, пустой message)
     * @throws Exception                при сетевых/низкоуровневых ошибках (HTTP/JSON/таймаут)
     */
    String generate(String message, String systemPrompt, String model) ;

}
