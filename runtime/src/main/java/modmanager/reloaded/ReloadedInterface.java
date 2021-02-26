package modmanager.reloaded;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

import java.io.File;

public interface ReloadedInterface extends StdCallLibrary {
    ReloadedInterface INSTANCE = Native.load(new File("lib\\ReloadedII\\Loader\\X86\\Bootstrapper\\Reloaded.Mod.Loader.Bootstrapper.dll").getAbsolutePath(), ReloadedInterface.class);

    boolean DllMain(WinDef.HMODULE hmodule, WinDef.DWORD ul_reason_for_call, WinDef.LPVOID lpReserved);
}
