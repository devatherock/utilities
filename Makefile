clean:
	./gradlew clean
integration-test:
	docker-compose up -d
	./gradlew check $(additional_gradle_args)
	docker-compose down