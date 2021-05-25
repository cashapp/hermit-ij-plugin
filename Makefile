PLUGIN_CHANNEL ?= canary
PLUGIN_VERSION ?= $(shell date +%Y%m%d%H%M)-canary

.PHONY: plugin
plugin:
	./bin/gradle buildPlugin -Pversion=$(PLUGIN_VERSION)
	sed -e s,PLUGIN_VERSION,$(PLUGIN_VERSION),g -e s,PLUGIN_CHANNEL,$(PLUGIN_CHANNEL),g updatePlugins.xml.tmpl > build/distributions/updatePlugins.xml
