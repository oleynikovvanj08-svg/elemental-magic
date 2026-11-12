Run gradle build --no-configuration-cache
Starting a Gradle Daemon (subsequent builds will be faster)
> Configure project :
Creating Minecraft artifacts without recompilation.
> Task :createMinecraftArtifacts
Loaded 131 artifacts from /home/runner/work/elemental-magic/elemental-magic/build/tmp/createMinecraftArtifacts/nfrt_artifact_manifest.properties
*** Started working on downloadJson
 ✓ Completed downloadJson in 0.02s
*** Started working on downloadClient
*** Started working on downloadClientMappings
*** Started working on downloadServer
 ✓ Completed downloadServer in 0.03s
 ✓ Completed downloadClient in 0.03s
 ✓ Completed downloadClientMappings in 0.03s
*** Started working on stripClient
 ♻ Used cache of stripClient in 0.00s
*** Started working on mergeMappings
 ♻ Used cache of mergeMappings in 0.00s
*** Started working on extractServer
 ♻ Used cache of extractServer in 0.00s
*** Started working on stripServer
 ♻ Used cache of stripServer in 0.00s
*** Started working on merge
 ♻ Used cache of merge in 0.00s
*** Started working on rename
 ♻ Used cache of rename in 0.00s
*** Started working on binaryPatch
 ♻ Used cache of binaryPatch in 0.00s
*** Started working on copyUnpatchedClasses
 ♻ Used cache of copyUnpatchedClasses in 0.00s
*** Started working on applyDevTransforms
 ♻ Used cache of applyDevTransforms in 0.00s
*** Started working on binaryWithNeoForge
 ♻ Used cache of binaryWithNeoForge in 0.00s
Total runtime: 2.85s
> Task :compileJava
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/SpellbookItem.java:56: error: cannot find symbol
        Map<String, Long> cds = stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of());
                                                              ^
  symbol:   variable SPELL_COOLDOWNS
  location: class ModRegistry
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/SpellbookItem.java:63: error: cannot find symbol
        Map<String, Long> cds = new HashMap<>(stack.getOrDefault(ModRegistry.SPELL_COOLDOWNS.get(), Map.of()));
                                                                            ^
  symbol:   variable SPELL_COOLDOWNS
  location: class ModRegistry
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/SpellbookItem.java:65: error: cannot find symbol
        stack.set(ModRegistry.SPELL_COOLDOWNS.get(), cds);
                             ^
  symbol:   variable SPELL_COOLDOWNS
  location: class ModRegistry
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/SpellbookItem.java:92: error: cannot find symbol
            msg = msg.append(Component.literal(" [" + String.format("%.1f", cdLeft / 20.0) + " с]")
                     ^
  symbol:   method append(MutableComponent)
  location: variable msg of type Component
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/network/ModNetwork.java:13: warning: [removal] bus() in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD)
                                            ^
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/network/ModNetwork.java:13: warning: [removal] Bus in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD)
                                                                    ^
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/client/ClientKeys.java:12: warning: [removal] bus() in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
                                            ^
/home/runner/work/elemental-magic/elemental-magic/src/main/java/com/example/elementalmagic/client/ClientKeys.java:12: warning: [removal] Bus in EventBusSubscriber has been deprecated and marked for removal
@EventBusSubscriber(modid = MagicMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
                                                                    ^
4 errors
4 warnings
> Task :compileJava FAILED
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_2-1794466825086.json
2 actionable tasks: 2 executed
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':compileJava'.
> Compilation failed; see the compiler error output for details.
* Try:
> Run with --info option to get more log output.
> Run with --scan to get full insights.
BUILD FAILED in 31s
Error: Process completed with exit code 1.
