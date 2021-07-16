APP=app

release:
	@npx shadow-cljs release $(APP)

dev:
	npx shadow-cljs watch $(APP)
