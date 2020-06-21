APP=app
SERVER=multiverse
NAME=motform/$(SERVER)
JAR=target/$(SERVER).jar

jar: clean cljs-compile uberjar runjar

cljs-compile:
	@npx shadow-cljs compile $(APP)

uberjar:
	@clojure -A:depstar -m hf.depstar.uberjar $(JAR) -S

runjar:
	@java -cp $(JAR) clojure.main -m cocktail.slurp.server

clean:
	@rm -rf target/ -q

dev:
	npx shadow-cljs watch $(APP) & clojure -A:server

.PHONY: dev
