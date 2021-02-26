(ns mod-applier.core
  (:require [clojure.java.io :as io])
  (:import org.apache.commons.io.FileUtils
           (java.nio.file Paths Files StandardCopyOption))
  (:gen-class
    :name modmanager.ModApplier
    :methods [#^{:static true}
              [createLinkTree [String String] boolean]]))

(def src-dir "F:\\LEGO Files\\LSWTCS\\PC")
(def mod-dir "F:\\LEGO Files\\LegoMods\\ModTest2")
(def dst-dir "F:\\Mods\\Game Instance")

(defn get-valid-files
  "Returns all files in the given directory, recursively, removing the root directory"
  [src]
  (map #(. (. % toString) replace src "")
       (filter #(not ( . % isDirectory)) (file-seq (io/file src)))))

(defn create-link-tree
  "Creates links for the files in the src directory in the dst directory"
  [src dst]
  (map (fn [path]
         (do (. (. (io/file (str dst path)) getParentFile) mkdirs)
             (Files/createLink (Paths/get dst (into-array String [path])) (Paths/get src (into-array String [path])))))
       (get-valid-files src)))

(defn apply-mod
  "Copies the files in the src directory in the dst directory"
  [mod target existing-state]
  (map (fn [path]
         (do (. (. (io/file (str target path)) getParentFile) mkdirs)
             (Files/copy (Paths/get mod (into-array String [path])) (Paths/get target (into-array String [path])) StandardCopyOption/REPLACE_EXISTING)
             path))
       (get-valid-files mod)))

(defn apply-mods
  "Applies the given mods given the existing repair state (ENGLISH.TXT entries)"
  [mods target existing-state]
  (if (empty? mods)
    true
    (apply-mods (rest mods) target (apply-mod (first mods) target existing-state))))

(defn create-game-instance
  [source target mods]
  (do
     (when (.exists (io/file target)) (. FileUtils deleteDirectory (io/file target)))
     (do
        (create-link-tree source target)
        (apply-mods mods target {}))))








(defn -createGameInstance [src dst mods]
  (create-game-instance src dst mods))

