# Публикация SilverVisualistic в GitHub и SilverVisible

## Перед первым push

1. Убедитесь, что проект собирается локально командой `gradle build` на Java 21.
2. Проверьте мод в Minecraft 1.21.4 Fabric.
3. Не добавляйте в репозиторий пароли SilverVisible, Cloudflare API tokens, webhook secrets или локальные файлы конфигурации.
4. Загрузите все файлы и папки проекта в корень репозитория `markdanil/SilverVisualistic`.
5. В GitHub откройте вкладку **Actions** и проверьте, что workflow **Build and publish SilverVisualistic** завершился успешно.

## Автоматический релиз

После успешного тестирования создайте и отправьте тег:

```bash
git tag v2.0.0
git push origin v2.0.0
```

Workflow соберёт мод на Java 21 и Gradle 9.8.2, приложит JAR и архив исходников к GitHub Release.

Важно: тег `v2.0.0` пока не отправлять, пока локальная сборка и тест в Minecraft не пройдены.

## Подключение SilverVisible webhook

1. В каталоге Cloudflare Worker выполните `npx.cmd wrangler secret put GITHUB_WEBHOOK_SECRET` и задайте длинный случайный секрет. Значение сохраните локально; не отправляйте его в чат и не коммитьте в Git.
2. В репозитории GitHub откройте **Settings → Webhooks → Add webhook**.
3. Payload URL: `https://silvervisible-api.silvervisible.workers.dev/v1/webhooks/github`.
4. Content type: `application/json`.
5. В поле Secret вставьте то же значение, что задали для Worker.
6. Выберите **Let me select individual events** и отметьте только **Releases**. Оставьте webhook активным.
7. После создания проверьте **Recent Deliveries**. Настоящий релиз должен появиться в разделе «Обновления» на сайте SilverVisible.

## Безопасность

- Не коммитьте секреты и токены.
- GitHub webhook должен проверять подпись HMAC; не отключайте проверку подписи.
- Публикуйте JAR только после успешного CI и игрового smoke-теста.
