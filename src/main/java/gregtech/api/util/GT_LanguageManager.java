package gregtech.api.util;

import static gregtech.api.enums.GT_Values.E;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import gregtech.api.GregTech_API;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class GT_LanguageManager {
    public static final HashMap<String, String> TEMPMAP = new HashMap<>(),
            BUFFERMAP = new HashMap<>(),
            LANGMAP = new HashMap<>();
    public static Configuration sEnglishFile;
    public static String sLanguage = "en_US";
    public static boolean sUseEnglishFile = false;
    public static boolean i18nPlaceholder = true;

    public static String FACE_ANY = "gt.lang.face.any",
            FACE_BOTTOM = "gt.lang.face.bottom",
            FACE_TOP = "gt.lang.face.top",
            FACE_LEFT = "gt.lang.face.left",
            FACE_FRONT = "gt.lang.face.front",
            FACE_RIGHT = "gt.lang.face.right",
            FACE_BACK = "gt.lang.face.back",
            FACE_NONE = "gt.lang.face.none";

    public static String[] FACES = {FACE_BOTTOM, FACE_TOP, FACE_LEFT, FACE_FRONT, FACE_RIGHT, FACE_BACK, FACE_NONE};

    private static Map<String, String> stringTranslateLanguageList = null;

    static {
        try {
            Field fieldStringTranslateLanguageList = ReflectionHelper.findField(
                    net.minecraft.util.StringTranslate.class, "languageList", "field_74816_c");
            Field fieldStringTranslateInstance =
                    ReflectionHelper.findField(net.minecraft.util.StringTranslate.class, "instance", "field_74817_a");
            //noinspection unchecked
            stringTranslateLanguageList =
                    (Map<String, String>) fieldStringTranslateLanguageList.get(fieldStringTranslateInstance.get(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String addStringLocalization(String aKey, String aEnglish) {
        return addStringLocalization(aKey, aEnglish, true);
    }

    public static synchronized String addStringLocalization(String aKey, String aEnglish, boolean aWriteIntoLangFile) {
        if (aKey == null) return E;
        if (aWriteIntoLangFile && (!LANGMAP.containsKey(aKey) || (sEnglishFile != null && !BUFFERMAP.isEmpty()))) {
            aEnglish = writeToLangFile(aKey, aEnglish);
            if (!LANGMAP.containsKey(aKey)) {
                LANGMAP.put(aKey, aEnglish);
                addToMCLangList(aKey, aEnglish);
            }
        }
        TEMPMAP.put(aKey.trim(), aEnglish);
        LanguageRegistry.instance().injectLanguage(sLanguage, TEMPMAP);
        TEMPMAP.clear();
        if (sUseEnglishFile && !aWriteIntoLangFile) {
            if (!LANGMAP.containsKey(aKey)) {
                Property tProperty = sEnglishFile.get("LanguageFile", aKey, aEnglish);
                aEnglish = tProperty.getString();
                LANGMAP.put(aKey, aEnglish);
                addToMCLangList(aKey, aEnglish);
            } else aEnglish = LANGMAP.get(aKey);
        }
        return aEnglish;
    }

    private static synchronized String writeToLangFile(String aKey, String aEnglish) {
        if (aKey == null) return E;
        if (sEnglishFile == null) {
            BUFFERMAP.put(aKey.trim(), aEnglish);
        } else {
            if (!BUFFERMAP.isEmpty()) {
                for (Entry<String, String> tEntry : BUFFERMAP.entrySet()) {
                    Property tProperty = sEnglishFile.get("LanguageFile", tEntry.getKey(), tEntry.getValue());
                    if (!tProperty.wasRead() && GregTech_API.sPostloadFinished) sEnglishFile.save();
                }
                BUFFERMAP.clear();
            }
            Property tProperty = sEnglishFile.get("LanguageFile", aKey.trim(), aEnglish);
            if (!tProperty.wasRead() && GregTech_API.sPostloadFinished) sEnglishFile.save();
            if (sEnglishFile
                    .get("EnableLangFile", "UseThisFileAsLanguageFile", false)
                    .getBoolean(false)) {
                aEnglish = tProperty.getString();
                sUseEnglishFile = true;
            }
        }
        return aEnglish;
    }

    public static String getTranslation(String aKey) {
        if (aKey == null) return E;
        String tTrimmedKey = aKey.trim(), rTranslation;
        if (sUseEnglishFile) {
            rTranslation = LanguageRegistry.instance().getStringLocalization(tTrimmedKey);
        } else {
            rTranslation = StatCollector.translateToLocal(tTrimmedKey);
        }
        if (GT_Utility.isStringInvalid(rTranslation)) {
            rTranslation = StatCollector.translateToLocal(tTrimmedKey);
            if (GT_Utility.isStringInvalid(rTranslation) || tTrimmedKey.equals(rTranslation)) {
                if (aKey.endsWith(".name")) {
                    String trimmedKeyNoName = tTrimmedKey.substring(0, tTrimmedKey.length() - 5);
                    rTranslation = StatCollector.translateToLocal(trimmedKeyNoName);
                    if (GT_Utility.isStringInvalid(rTranslation) || trimmedKeyNoName.equals(rTranslation)) {
                        return aKey;
                    }
                } else {
                    rTranslation = StatCollector.translateToLocal(tTrimmedKey + ".name");
                    if (GT_Utility.isStringInvalid(rTranslation) || (tTrimmedKey + ".name").equals(rTranslation)) {
                        return aKey;
                    }
                }
            }
        }
        return rTranslation;
    }

    public static String getTranslation(String aKey, String aSeperator) {
        if (aKey == null) return E;
        String rTranslation = E;
        StringBuilder rTranslationSB = new StringBuilder(rTranslation);
        for (String tString : aKey.split(aSeperator)) {
            rTranslationSB.append(getTranslation(tString));
        }
        rTranslation = String.valueOf(rTranslationSB);
        return rTranslation;
    }

    public static String getTranslateableItemStackName(ItemStack aStack) {
        if (GT_Utility.isStackInvalid(aStack)) return "null";
        NBTTagCompound tNBT = aStack.getTagCompound();
        if (tNBT != null && tNBT.hasKey("display")) {
            String tName = tNBT.getCompoundTag("display").getString("Name");
            if (GT_Utility.isStringValid(tName)) {
                return tName;
            }
        }
        return aStack.getUnlocalizedName() + ".name";
    }

    public static void writePlaceholderStrings() {
        addStringLocalization("Interaction_DESCRIPTION_Index_001", "Puts out into adjacent Slot #");
        addStringLocalization("Interaction_DESCRIPTION_Index_002", "Grabs in for own Slot #");
        addStringLocalization("Interaction_DESCRIPTION_Index_003", "Enable with Signal");
        addStringLocalization("Interaction_DESCRIPTION_Index_004", "Disable with Signal");
        addStringLocalization("Interaction_DESCRIPTION_Index_005", "Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_006", "Export");
        addStringLocalization("Interaction_DESCRIPTION_Index_007", "Import");
        addStringLocalization("Interaction_DESCRIPTION_Index_008", "Export (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_009", "Import (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_010", "Export (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_011", "Import (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_012", "Export allow Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_013", "Import allow Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_014", "Export allow Input (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_015", "Import allow Output (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_016", "Export allow Input (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_017", "Import allow Output (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_018", "Normal");
        addStringLocalization("Interaction_DESCRIPTION_Index_019", "Inverted");
        addStringLocalization("Interaction_DESCRIPTION_Index_020", "Ready to work");
        addStringLocalization("Interaction_DESCRIPTION_Index_021", "Not ready to work");
        addStringLocalization("Interaction_DESCRIPTION_Index_022", "Import");
        addStringLocalization("Interaction_DESCRIPTION_Index_023", "Import (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_024", "Import (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_025", "Keep Liquids Away");
        addStringLocalization("Interaction_DESCRIPTION_Index_026", "Keep Liquids Away (conditional)");
        addStringLocalization("Interaction_DESCRIPTION_Index_027", "Keep Liquids Away (invert cond)");
        addStringLocalization("Interaction_DESCRIPTION_Index_031", "Normal Universal Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_032", "Inverted Universal Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_033", "Normal Electricity Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_034", "Inverted Electricity Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_035", "Normal Steam Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_036", "Inverted Steam Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_037", "Normal Average Electric Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_038", "Inverted Average Electric Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_039", "Normal Average Electric Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_040", "Inverted Average Electric Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_041", "Normal Electricity Storage(Including Batteries)");
        addStringLocalization("Interaction_DESCRIPTION_Index_042", "Inverted Electricity Storage(Including Batteries)");
        addStringLocalization("Interaction_DESCRIPTION_Index_043", "Filter input, Deny output");
        addStringLocalization("Interaction_DESCRIPTION_Index_044", "Invert input, Deny output");
        addStringLocalization("Interaction_DESCRIPTION_Index_045", "Filter input, Permit any output");
        addStringLocalization("Interaction_DESCRIPTION_Index_046", "Invert input, Permit any output");
        addStringLocalization("Interaction_DESCRIPTION_Index_047", "Filter Fluid: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_048", "Pump speed: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_049", "L/tick ");
        addStringLocalization("Interaction_DESCRIPTION_Index_050", "L/sec");
        addStringLocalization("Interaction_DESCRIPTION_Index_053", "Slot: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_054", "Inverted");
        addStringLocalization("Interaction_DESCRIPTION_Index_055", "Normal");
        addStringLocalization("Interaction_DESCRIPTION_Index_056", "Emit if 1 Maintenance Needed");
        addStringLocalization("Interaction_DESCRIPTION_Index_057", "Emit if 1 Maintenance Needed(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_058", "Emit if 2 Maintenance Needed");
        addStringLocalization("Interaction_DESCRIPTION_Index_059", "Emit if 2 Maintenance Needed(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_060", "Emit if 3 Maintenance Needed");
        addStringLocalization("Interaction_DESCRIPTION_Index_061", "Emit if 3 Maintenance Needed(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_062", "Emit if 4 Maintenance Needed");
        addStringLocalization("Interaction_DESCRIPTION_Index_063", "Emit if 4 Maintenance Needed(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_064", "Emit if 5 Maintenance Needed");
        addStringLocalization("Interaction_DESCRIPTION_Index_065", "Emit if 5 Maintenance Needed(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_066", "Emit if rotor needs maintenance low accuracy mod");
        addStringLocalization(
                "Interaction_DESCRIPTION_Index_067", "Emit if rotor needs maintenance low accuracy mod(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_068", "Emit if rotor needs maintenance high accuracy mod");
        addStringLocalization("Interaction_DESCRIPTION_Index_068.1", "Emit if any Player is close");
        addStringLocalization(
                "Interaction_DESCRIPTION_Index_069", "Emit if rotor needs maintenance high accuracy mod(inverted)");
        addStringLocalization("Interaction_DESCRIPTION_Index_069.1", "Emit if other Player is close");
        addStringLocalization("Interaction_DESCRIPTION_Index_070", "Emit if you are close");
        addStringLocalization("Interaction_DESCRIPTION_Index_071", "Conducts strongest Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_072", "Conducts from bottom Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_073", "Conducts from top Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_074", "Conducts from north Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_075", "Conducts from south Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_076", "Conducts from west Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_077", "Conducts from east Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_078", "Signal = ");
        addStringLocalization("Interaction_DESCRIPTION_Index_079", "Conditional Signal = ");
        addStringLocalization("Interaction_DESCRIPTION_Index_080", "Inverted Conditional Signal = ");
        addStringLocalization("Interaction_DESCRIPTION_Index_081", "Frequency: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_082", "Open if work enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_083", "Open if work disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_084", "Only Output allowed");
        addStringLocalization("Interaction_DESCRIPTION_Index_085", "Only Input allowed");
        addStringLocalization("Interaction_DESCRIPTION_Index_086", "Auto-Input: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_087", "Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_088", "Enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_089", "  Auto-Output: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_090", "Machine Processing: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_091", "Redstone Output at Side ");
        addStringLocalization("Interaction_DESCRIPTION_Index_092", " set to: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_093", "Strong");
        addStringLocalization("Interaction_DESCRIPTION_Index_094", "Weak");
        addStringLocalization("Interaction_DESCRIPTION_Index_095", "Input from Output Side allowed");
        addStringLocalization("Interaction_DESCRIPTION_Index_096", "Input from Output Side forbidden");
        addStringLocalization("Interaction_DESCRIPTION_Index_098", "Do not regulate Item Stack Size");
        addStringLocalization("Interaction_DESCRIPTION_Index_099", "Regulate Item Stack Size to: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_100", "This is ");
        addStringLocalization("Interaction_DESCRIPTION_Index_101", " Ore.");
        addStringLocalization("Interaction_DESCRIPTION_Index_102", "There is Lava behind this Rock.");
        addStringLocalization("Interaction_DESCRIPTION_Index_103", "There is a Liquid behind this Rock.");
        addStringLocalization("Interaction_DESCRIPTION_Index_104", "There is an Air Pocket behind this Rock.");
        addStringLocalization("Interaction_DESCRIPTION_Index_105", "Material is changing behind this Rock.");
        addStringLocalization("Interaction_DESCRIPTION_Index_106", "Found traces of ");
        addStringLocalization("Interaction_DESCRIPTION_Index_107", "No Ores found.");
        addStringLocalization("Interaction_DESCRIPTION_Index_108", "Outputs misc. Fluids, Steam and Items");
        addStringLocalization("Interaction_DESCRIPTION_Index_109", "Outputs Steam and Items");
        addStringLocalization("Interaction_DESCRIPTION_Index_110", "Outputs Steam and misc. Fluids");
        addStringLocalization("Interaction_DESCRIPTION_Index_111", "Outputs Steam");
        addStringLocalization("Interaction_DESCRIPTION_Index_112", "Outputs misc. Fluids and Items");
        addStringLocalization("Interaction_DESCRIPTION_Index_113", "Outputs only Items");
        addStringLocalization("Interaction_DESCRIPTION_Index_114", "Outputs only misc. Fluids");
        addStringLocalization("Interaction_DESCRIPTION_Index_115", "Outputs nothing");
        addStringLocalization("Interaction_DESCRIPTION_Index_116", "Emit Energy to Outputside");
        addStringLocalization("Interaction_DESCRIPTION_Index_117", "Don't emit Energy");
        addStringLocalization("Interaction_DESCRIPTION_Index_118", "Emit Redstone if no Slot is free");
        addStringLocalization("Interaction_DESCRIPTION_Index_119", "Don't emit Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_120", "Invert Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_121", "Don't invert Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_122", "Emit Redstone if slots contain something");
        addStringLocalization("Interaction_DESCRIPTION_Index_123", "Don't emit Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_124", "Invert Filter");
        addStringLocalization("Interaction_DESCRIPTION_Index_124.1", "Blacklist Mode");
        addStringLocalization("Interaction_DESCRIPTION_Index_125", "Don't invert Filter");
        addStringLocalization("Interaction_DESCRIPTION_Index_125.1", "Whitelist Mode");
        addStringLocalization("Interaction_DESCRIPTION_Index_126", "Ignore NBT");
        addStringLocalization("Interaction_DESCRIPTION_Index_127", "NBT has to match");
        addStringLocalization("Interaction_DESCRIPTION_Index_128", "Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_128.1", "Redstone ");
        addStringLocalization("Interaction_DESCRIPTION_Index_129", "Energy");
        addStringLocalization("Interaction_DESCRIPTION_Index_129.1", "Energy ");
        addStringLocalization("Interaction_DESCRIPTION_Index_130", "Fluids");
        addStringLocalization("Interaction_DESCRIPTION_Index_130.1", "Fluids ");
        addStringLocalization("Interaction_DESCRIPTION_Index_131", "Items");
        addStringLocalization("Interaction_DESCRIPTION_Index_131.1", "Items ");
        addStringLocalization("Interaction_DESCRIPTION_Index_132", "Pipe is loose.");
        addStringLocalization("Interaction_DESCRIPTION_Index_133", "Screws are loose.");
        addStringLocalization("Interaction_DESCRIPTION_Index_134", "Something is stuck.");
        addStringLocalization("Interaction_DESCRIPTION_Index_135", "Platings are dented.");
        addStringLocalization("Interaction_DESCRIPTION_Index_136", "Circuitry burned out.");
        addStringLocalization("Interaction_DESCRIPTION_Index_137", "That doesn't belong there.");
        addStringLocalization("Interaction_DESCRIPTION_Index_138", "Incomplete Structure.");
        addStringLocalization("Interaction_DESCRIPTION_Index_139", "Hit with Soft Mallet");
        addStringLocalization("Interaction_DESCRIPTION_Index_140", "to (re-)start the Machine");
        addStringLocalization("Interaction_DESCRIPTION_Index_141", "if it doesn't start.");
        addStringLocalization("Interaction_DESCRIPTION_Index_142", "Running perfectly.");
        addStringLocalization("Interaction_DESCRIPTION_Index_143", "Missing Mining Pipe");
        addStringLocalization("Interaction_DESCRIPTION_Index_144", "Missing Turbine Rotor");
        addStringLocalization("Interaction_DESCRIPTION_Index_145", "Step Down, In: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_146", "Step Up, In: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_147", "A, Out: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_148", "V ");
        addStringLocalization("Interaction_DESCRIPTION_Index_149", "A");
        addStringLocalization("Interaction_DESCRIPTION_Index_150", "Chance: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_151", "Does not get consumed in the process");
        addStringLocalization("Interaction_DESCRIPTION_Index_151.1", "Outputs items and 1 specific Fluid");
        addStringLocalization("Interaction_DESCRIPTION_Index_151.2", "Outputs 1 specific Fluid");
        addStringLocalization("Interaction_DESCRIPTION_Index_151.4", "Successfully locked Fluid to %s");
        addStringLocalization("Interaction_DESCRIPTION_Index_152", "Total: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_152.1", "Max EU: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_153", "Usage: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_154", "Voltage: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_155", "Amperage: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_156", "Voltage: unspecified");
        addStringLocalization("Interaction_DESCRIPTION_Index_157", "Amperage: unspecified");
        addStringLocalization("Interaction_DESCRIPTION_Index_158", "Time: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_159", "Needs Low Gravity");
        addStringLocalization("Interaction_DESCRIPTION_Index_160", "Needs Cleanroom");
        addStringLocalization("Interaction_DESCRIPTION_Index_160.1", "Needs Cleanroom & LowGrav");
        addStringLocalization("Interaction_DESCRIPTION_Index_161", " secs");
        addStringLocalization("Interaction_DESCRIPTION_Index_162", "Name: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_163", " MetaData: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_164", "Hardness: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_165", " Blast Resistance: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_166", "Is valid Beacon Pyramid Material");
        addStringLocalization("Interaction_DESCRIPTION_Index_167", "Tank ");
        addStringLocalization("Interaction_DESCRIPTION_Index_168", "Heat: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_169", "HEM: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_170", " Base EU Output: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_171", "Facing: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_172", " / Chance: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_173", "You can remove this with a Wrench");
        addStringLocalization("Interaction_DESCRIPTION_Index_174", "You can NOT remove this with a Wrench");
        addStringLocalization("Interaction_DESCRIPTION_Index_175", "Conduction Loss: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_176", "Contained Energy: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_177", "Has Muffler Upgrade");
        addStringLocalization("Interaction_DESCRIPTION_Index_178", "Progress/Load: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_179", "Max IN: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_181", "Max OUT: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_182", " EU at ");
        addStringLocalization("Interaction_DESCRIPTION_Index_183", " A");
        addStringLocalization("Interaction_DESCRIPTION_Index_184", "Energy: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_186", "Owned by: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_187", "Type -- Crop-Name: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_188", "  Growth: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_189", "  Gain: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_190", "  Resistance: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_191", "Plant -- Fertilizer: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_192", "  Water: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_193", "  Weed-Ex: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_194", "  Scan-Level: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_195", "Environment -- Nutrients: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_196", "  Humidity: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_197", "  Air-Quality: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_198", "Attributes:");
        addStringLocalization("Interaction_DESCRIPTION_Index_199", "Discovered by: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_200", "Sort mode: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_200.1", "Automatic Item Shuffling: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_201", "Nothing");
        addStringLocalization("Interaction_DESCRIPTION_Index_202", "Pollution in Chunk: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_203", " gibbl");
        addStringLocalization("Interaction_DESCRIPTION_Index_204", "No Pollution in Chunk! HAYO!");
        addStringLocalization("Interaction_DESCRIPTION_Index_206", "Scan for Assembly Line");
        addStringLocalization(
                "Interaction_DESCRIPTION_Index_207", "Pump speed: %dL every %d ticks, %.2f L/sec on average");
        addStringLocalization("Interaction_DESCRIPTION_Index_208", " L");
        addStringLocalization("Interaction_DESCRIPTION_Index_209", " ticks");
        addStringLocalization("Interaction_DESCRIPTION_Index_209.1", " ticks");
        addStringLocalization("Interaction_DESCRIPTION_Index_210", "Average: %.2f L/sec");
        addStringLocalization("Interaction_DESCRIPTION_Index_211", "Items per side: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_212", "Input enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_213", "Input disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_214", "Connected");
        addStringLocalization("Interaction_DESCRIPTION_Index_215", "Disconnected");
        addStringLocalization("Interaction_DESCRIPTION_Index_216", "Deprecated Recipe");
        addStringLocalization("Interaction_DESCRIPTION_Index_219", "Extended Facing: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_220", "Single recipe locking disabled.");
        addStringLocalization("Interaction_DESCRIPTION_Index_221", "Item threshold");
        addStringLocalization("Interaction_DESCRIPTION_Index_222", "Fluid threshold");
        addStringLocalization("Interaction_DESCRIPTION_Index_222.1", "Energy threshold");
        addStringLocalization(
                "Interaction_DESCRIPTION_Index_223", "Single recipe locking enabled. Will lock to next recipe.");
        addStringLocalization("Interaction_DESCRIPTION_Index_224", "Always On");
        addStringLocalization("Interaction_DESCRIPTION_Index_225", "Active with Redstone Signal");
        addStringLocalization("Interaction_DESCRIPTION_Index_226", "Inactive with Redstone Signal");
        addStringLocalization("Interaction_DESCRIPTION_Index_227", "Allow Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_228", "Block Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_229", "Import/Export");
        addStringLocalization("Interaction_DESCRIPTION_Index_230", "Conditional");
        addStringLocalization("Interaction_DESCRIPTION_Index_231", "Enable Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_232", "Filter Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_233", "Filter Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_234", "Block Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_235", "Allow Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_236", "Whitelist Fluid");
        addStringLocalization("Interaction_DESCRIPTION_Index_237", "Blacklist Fluid");
        addStringLocalization("Interaction_DESCRIPTION_Index_238", "Filter Direction");
        addStringLocalization("Interaction_DESCRIPTION_Index_239", "Filter Type");
        addStringLocalization("Interaction_DESCRIPTION_Index_240", "Block Flow");
        addStringLocalization("Interaction_DESCRIPTION_Index_241", "Recipe progress");
        addStringLocalization("Interaction_DESCRIPTION_Index_242", "Machine idle");
        addStringLocalization("Interaction_DESCRIPTION_Index_243", "Enable with Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_244", "Disable with Redstone");
        addStringLocalization("Interaction_DESCRIPTION_Index_245", "Disable machine");
        addStringLocalization("Interaction_DESCRIPTION_Index_246", "Frequency");
        addStringLocalization("Interaction_DESCRIPTION_Index_247", "1 Issue");
        addStringLocalization("Interaction_DESCRIPTION_Index_248", "2 Issues");
        addStringLocalization("Interaction_DESCRIPTION_Index_249", "3 Issues");
        addStringLocalization("Interaction_DESCRIPTION_Index_250", "4 Issues");
        addStringLocalization("Interaction_DESCRIPTION_Index_251", "5 Issues");
        addStringLocalization("Interaction_DESCRIPTION_Index_252", "Rotor < 80%");
        addStringLocalization("Interaction_DESCRIPTION_Index_253", "Rotor < 100%");
        addStringLocalization("Interaction_DESCRIPTION_Index_254", "Detect slot#");
        addStringLocalization("Interaction_DESCRIPTION_Index_254.1", "Internal slot#");
        addStringLocalization("Interaction_DESCRIPTION_Index_255", "Adjacent slot#");
        addStringLocalization("Interaction_DESCRIPTION_Index_256", "Universal Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_257", "Electricity Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_258", "Steam Storage");
        addStringLocalization("Interaction_DESCRIPTION_Index_259", "Average Electric Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_260", "Average Electric Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_261", "Electricity Storage(Including Batteries)");
        addStringLocalization("Interaction_DESCRIPTION_Index_262", "Fluid Auto Output Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_263", "Fluid Auto Output Enabled");
        addStringLocalization(
                "Interaction_DESCRIPTION_Index_264", "currently none, will be locked to the next that is put in");
        addStringLocalization("Interaction_DESCRIPTION_Index_265", "1 specific Fluid");
        addStringLocalization("Interaction_DESCRIPTION_Index_266", "Lock Fluid Mode Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_267", "Overflow Voiding Mode Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_268", "Overflow Voiding Mode Enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_269", "Void Full Mode Disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_270", "Void Full Mode Enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_271", "unspecified");
        addStringLocalization("Interaction_DESCRIPTION_Index_272", "Recipe by: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_273", "Original Recipe by: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_274", "Modified by: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_275", "Original voltage: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_299", "Item Filter: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_300", "Filter Cleared!");
        addStringLocalization("Interaction_DESCRIPTION_Index_300.1", "Fluid Lock Cleared.");
        addStringLocalization("Interaction_DESCRIPTION_Index_301", "Universal");
        addStringLocalization("Interaction_DESCRIPTION_Index_302", "Int. EU");
        addStringLocalization("Interaction_DESCRIPTION_Index_303", "Steam");
        addStringLocalization("Interaction_DESCRIPTION_Index_304", "Avg. Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_305", "Avg. Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_306", "EU stored");
        addStringLocalization("Interaction_DESCRIPTION_Index_307", "Deny input, Filter output");
        addStringLocalization("Interaction_DESCRIPTION_Index_308", "Deny input, Invert output");
        addStringLocalization("Interaction_DESCRIPTION_Index_309", "Permit any input, Filter output");
        addStringLocalization("Interaction_DESCRIPTION_Index_310", "Permit any input, Invert output");
        addStringLocalization("Interaction_DESCRIPTION_Index_311", "Block Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_312", "Allow Output");
        addStringLocalization("Interaction_DESCRIPTION_Index_313", "Block Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_314", "Allow Input");
        addStringLocalization("Interaction_DESCRIPTION_Index_315", "Filter Empty");
        addStringLocalization("Interaction_DESCRIPTION_Index_316", "Pump speed limit reached!");
        addStringLocalization("Interaction_DESCRIPTION_Index_317", "Filter: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_318", "Check Mode");
        addStringLocalization("Interaction_DESCRIPTION_Index_319", "Any player");
        addStringLocalization("Interaction_DESCRIPTION_Index_320", "Other players");
        addStringLocalization("Interaction_DESCRIPTION_Index_321", "Only owner");
        addStringLocalization("Interaction_DESCRIPTION_Index_322", "Overflow point: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_323", "L");
        addStringLocalization("Interaction_DESCRIPTION_Index_324", "Now");
        addStringLocalization("Interaction_DESCRIPTION_Index_325", "Max");
        addStringLocalization("Interaction_DESCRIPTION_Index_326", "Public");
        addStringLocalization("Interaction_DESCRIPTION_Index_327", "Private");
        addStringLocalization("Interaction_DESCRIPTION_Index_328", "Channel");
        addStringLocalization("Interaction_DESCRIPTION_Index_329", "Public/Private");
        addStringLocalization("Interaction_DESCRIPTION_Index_330", "Sneak Rightclick to switch Mode");
        addStringLocalization("Interaction_DESCRIPTION_Index_500", "Fitting: Loose - More Flow");
        addStringLocalization("Interaction_DESCRIPTION_Index_501", "Fitting: Tight - More Efficiency");
        addStringLocalization("Interaction_DESCRIPTION_Index_502", "Mining chunk loading enabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_503", "Mining chunk loading disabled");
        addStringLocalization("Interaction_DESCRIPTION_Index_505", "Enable with Signal (Safe)");
        addStringLocalization("Interaction_DESCRIPTION_Index_506", "Disable with Signal (Safe)");
        addStringLocalization("Interaction_DESCRIPTION_Index_507", "Safe Mode");
        addStringLocalization("Interaction_DESCRIPTION_Index_601", "Use Private Frequency");
        addStringLocalization("Interaction_DESCRIPTION_Index_756", "Connectable: ");
        addStringLocalization("Interaction_DESCRIPTION_Index_ALL", "All");
        addStringLocalization("Interaction_DESCRIPTION_Index_ANY", "Any");
        addStringLocalization("Interaction_DESCRIPTION_Index_INVERTED", "Inverted");
        addStringLocalization("Interaction_DESCRIPTION_Index_NORMAL", "Normal");
        addStringLocalization("Interaction_DESCRIPTION_Index_SIDE", "Side: ");

        addStringLocalization("Item_DESCRIPTION_Index_000", "Stored Heat: %s");
        addStringLocalization("Item_DESCRIPTION_Index_001", "Durability: %s/%s");
        addStringLocalization("Item_DESCRIPTION_Index_002", "%s lvl %s");
        addStringLocalization("Item_DESCRIPTION_Index_003", "Attack Damage: %s");
        addStringLocalization("Item_DESCRIPTION_Index_004", "Mining Speed: %s");
        addStringLocalization("Item_DESCRIPTION_Index_005", "Turbine Efficiency: %s");
        addStringLocalization("Item_DESCRIPTION_Index_006", "Optimal Steam flow: %sL/sec");
        addStringLocalization("Item_DESCRIPTION_Index_007", "Optimal Gas flow(EU burnvalue per tick): %sEU/t");
        addStringLocalization("Item_DESCRIPTION_Index_008", "Optimal Plasma flow(Plasma energyvalue per tick): %sEU/t");
        addStringLocalization("Item_DESCRIPTION_Index_009", "Contains %s EU   Tier: %s");
        addStringLocalization("Item_DESCRIPTION_Index_010", "Empty. You should recycle it properly.");
        addStringLocalization("Item_DESCRIPTION_Index_011", "%s / %s EU - Voltage: %s");
        addStringLocalization("Item_DESCRIPTION_Index_012", "No Fluids Contained");
        addStringLocalization("Item_DESCRIPTION_Index_013", "%sL / %sL");
        addStringLocalization("Item_DESCRIPTION_Index_014", "Missing Coodinates!");
        addStringLocalization("Item_DESCRIPTION_Index_015", "Device at:");
        addStringLocalization("Item_DESCRIPTION_Index_016", "Amount: %s L");
        addStringLocalization("Item_DESCRIPTION_Index_017", "Temperature: %s K");
        addStringLocalization("Item_DESCRIPTION_Index_018", "State: %s");
        addStringLocalization("Item_DESCRIPTION_Index_500", "Turbine Efficiency (Loose): %s");
        addStringLocalization("Item_DESCRIPTION_Index_501", "Optimal Steam flow (Loose): %s L/t");

        addStringLocalization(FACE_ANY, "Any Side");
        addStringLocalization(FACE_BOTTOM, "Bottom");
        addStringLocalization(FACE_TOP, "Top");
        addStringLocalization(FACE_LEFT, "Left");
        addStringLocalization(FACE_FRONT, "Front");
        addStringLocalization(FACE_RIGHT, "Right");
        addStringLocalization(FACE_BACK, "Back");
        addStringLocalization(FACE_NONE, "None");
    }

    private static void addToMCLangList(String aKey, String aEnglish) {
        if (stringTranslateLanguageList != null) {
            stringTranslateLanguageList.put(aKey, aEnglish);
        }
    }
}
