APP_TITID := $(shell grep -oP '"tid"\s*:\s*"\K(\w+)' $(CURDIR)/emuiibo/toolbox.json)
APP_TITLE = emuiibo
export APP_TITLE

TARGET_TRIPLE := aarch64-nintendo-switch-freestanding

.PHONY: all emuiibo sysmodule overlay emuiigen clean emuiibo-clean emuiigen-clean

all: emuiibo emuiigen

clean: emuiibo-clean emuiigen-clean

emuiibo: sysmodule overlay
	@rm -rf $(CURDIR)/SdOut
	@mkdir -p $(CURDIR)/SdOut/atmosphere/contents/$(APP_TITID)/flags
	@touch $(CURDIR)/SdOut/atmosphere/contents/$(APP_TITID)/flags/boot2.flag
	@cp $(CURDIR)/emuiibo/target/$(TARGET_TRIPLE)/release/$(APP_TITLE).nsp $(CURDIR)/SdOut/atmosphere/contents/$(APP_TITID)/exefs.nsp
	@cp $(CURDIR)/emuiibo/toolbox.json $(CURDIR)/SdOut/atmosphere/contents/$(APP_TITID)/toolbox.json
	@mkdir -p $(CURDIR)/SdOut/switch/.overlays/lang/$(APP_TITLE)
	@cp -f $(CURDIR)/overlay/$(APP_TITLE).ovl $(CURDIR)/SdOut/switch/.overlays/$(APP_TITLE).ovl
	@cp -rf $(CURDIR)/overlay/lang/* $(CURDIR)/SdOut/switch/.overlays/lang/$(APP_TITLE)/
	@cd $(CURDIR)/SdOut; zip -r -q -9 $(APP_TITLE).zip atmosphere switch; cd $(CURDIR)

sysmodule:
	@cd emuiibo && cargo nx build --release

overlay:
	@$(MAKE) -C overlay/ all

emuiigen:
	@cd emuiigen && mvn package
emuiibo-clean:
	@rm -rf $(CURDIR)/SdOut
	@cd emuiibo && cargo clean
	@$(MAKE) clean -C overlay/
emuiigen-clean:
	@cd emuiigen && mvn clean
