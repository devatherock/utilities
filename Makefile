clean:
	rm -rf build
integration-test:
	docker compose up --wait
	./gradlew check $(additional_gradle_args)
	docker-compose down