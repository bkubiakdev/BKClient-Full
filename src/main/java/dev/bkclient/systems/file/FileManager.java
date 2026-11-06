package dev.bkclient.systems.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import dev.bkclient.systems.file.impl.ClientDataFile;
import dev.bkclient.systems.modules.modules.visuals.ViewModel;
import net.minecraft.client.MinecraftClient;

public class FileManager {
   public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "BK-Client");
   public static final String DEFAULT_FILE_TYPE = "bk";
   private final List<ClientFile> clientFiles = new ArrayList<>();

   public FileManager() {
      try {
         LegacyDataMigration.migrate(MinecraftClient.getInstance().runDirectory.toPath());
         if (!DIRECTORY.exists()) {
            Files.createDirectories(Path.of(DIRECTORY.toURI()));
         }
      } catch (IOException var2) {
         System.err.println("Error creating directory: " + var2.getMessage());
      }
   }

   public void registerClientFiles() {
      this.clientFiles.add(new ClientDataFile());
      this.clientFiles.add(new ViewModel.ViewModelFile());
   }

   public ClientFile getClientFile(String clientFileName) {
      return this.clientFiles.stream().filter(clientFile -> clientFile.getInfoAnnotation().name().equalsIgnoreCase(clientFileName)).findFirst().orElse(null);
   }

   public void readFile(ClientFile clientFile) {
      try {
         if (clientFile.getFile().exists() || Files.exists(SafeJsonFile.backup(clientFile.getFile().toPath()))) {
            clientFile.read();
         }
      } catch (Exception var3) {
         System.err.println("Error reading file: " + var3.getMessage());
      }
   }

   public void readFile(String clientFileName) {
      ClientFile clientFile = this.getClientFile(clientFileName);
      if (clientFile != null) {
         this.readFile(clientFile);
      }
   }

   public void writeFile(ClientFile clientFile) {
      if (dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave || dev.bkclient.BKClientInit.getInstance().isPanic()) return;
      try {
         clientFile.write();
      } catch (Exception var3) {
         System.err.println("Error saving file: " + var3.getMessage());
      }
   }

   public void writeFile(String clientFileName) {
      if (dev.bkclient.systems.setting.impl.AbstractSetting.suppressSave || dev.bkclient.BKClientInit.getInstance().isPanic()) return;
      ClientFile clientFile = this.getClientFile(clientFileName);
      if (clientFile != null) {
         clientFile.write();
      }
   }

   public void loadClientFiles() {
      for (ClientFile file : this.clientFiles) {
         this.readFile(file);
      }
   }

   public void saveClientFiles() {
      for (ClientFile file : this.clientFiles) {
         this.writeFile(file);
      }
   }

   @Generated
   public List<ClientFile> getClientFiles() {
      return this.clientFiles;
   }
}
