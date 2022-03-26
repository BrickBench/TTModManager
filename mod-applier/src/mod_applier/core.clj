(ns mod-applier.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.data :as data])
  (:import (java.nio.file Paths Files)
           (java.nio.file.attribute FileAttribute))
  (:gen-class
    :name modmanager.instance.ModApplier
    :methods [#^{:static true}
              [createLinkTree [String String] void]]))

(def src-dir "F:\\LEGO Files\\LSWTCS\\PC Edited")
(def mod-dir "C:\\Users\\javst\\Documents\\TT Mod Manager\\Mods\\SOLOALEGOSTARWARSCHARSPACK")
(def dst-dir "C:\\Users\\javst\\Documents\\TT Mod Manager\\Game Instance")

(def copy-files ["ENGLISH.TXT" "CHARS.TXT" "COLLECTION.TXT"])
(def ignore-files ["AI.PAL" "HAT_HAIR_ALL_PC.GSC" "HEAD_ALL_PC.GSC" "alltxt.pak"])

(def char-regex #"(?m)char_start\s*dir\s*\"(.*)\"\s*file\s*\"(.*)\"\s*char_end")
(def char-name-regex #"name_id[\s=](\d*)\s*\n")

(defn delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  (when (.isDirectory file)
    (doseq [file-in-dir (.listFiles file)]
      (delete-directory-recursive file-in-dir)))
  (io/delete-file file))

(defn parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(defn get-valid-files
  "Returns all files in the given directory, recursively, removing the root directory
  and ignoring any files in ignore-files."
  [src]
  (doall (filter (fn [path] (not (some #(str/ends-with? (str/lower-case path) %) ignore-files)))
           (map #( . (. % toString) replace src "")
                (filter #(not ( . % isDirectory)) (file-seq (io/file src)))))))

(defn create-link-tree
  "Creates links for the files in the src directory in the dst directory.
  This also does a direct copy for any file in the vector copy-files."
  [src dst]
  (doall (map (fn [path]
                (do (. (. (io/file (str dst path)) getParentFile) mkdirs)
                    (if (some #(str/ends-with? (str/lower-case path) %) copy-files)
                      (io/copy (io/file src path) (io/file dst path))
                      (Files/createSymbolicLink (Paths/get dst (into-array String [path]))
                                                (Paths/get src (into-array String [path]))
                                                (make-array FileAttribute 0)))))
              (get-valid-files src))))

(defn parse-collections
  [target mod loaded-data]
  loaded-data)

(defn parse-english
  [target mod loaded-data]
  loaded-data)

(defn correct-char-indices
  "Corrects character indices to fit in the empty region in the console
   information section, returning the newly adjusted load data."
  [char-file loaded-data]
  (let
       [char-data (slurp char-file)
        match-str (re-find char-name-regex char-data)
        existing-index (first (rest match-str))]
       (if (contains? (:index-changes loaded-data) existing-index)
           (do (spit char-file
                 (str/replace char-data
                              (first match-str)
                              (str "name_id=" ((:index-changes loaded-data) existing-index)  "\n")))
             loaded-data)

           (do (spit char-file
                 (str/replace char-data
                              (first match-str)
                              (str "name_id=" (:character-index loaded-data) "\n")))
             (assoc loaded-data
               :index-changes (merge (:index-changes loaded-data)
                                     {(parse-int existing-index) (+ 1 (:character-index loaded-data))})
               :character-index (+ 1 (:character-index loaded-data)))))))


(defn parse-chars
  "Parses a chars.txt file into a set of entries in [dir file] format"
  [char-file]
   (map #(into [] (rest %))
        (re-seq char-regex (slurp char-file))))

(defn process-chars
  "Processes a mod chars.txt file, appending its new characters onto
  the :chars set and correcting the name indices for new characters"
  [dst mod src loaded-data]
  (let [mod-chars (parse-chars (str mod "\\chars\\chars.txt"))
        original-chars (parse-chars (str src "\\chars\\chars.txt"))
        mod-char-files (map #(str dst "\\chars\\" (nth % 0) "\\" (nth % 1) ".txt")
                            (filter some? (first (data/diff mod-chars original-chars))))
        corrected-data (reduce #(correct-char-indices %2 %1) loaded-data mod-char-files)]

    (assoc corrected-data
      :chars (set/union (:chars corrected-data) mod-chars))))


(defn apply-mod
  "Copies the files in the src directory in the dst directory"
  [dst src mod loaded-data]
  (reduce #(cond
             (str/ends-with? (str/lower-case %2) "chars.txt") (process-chars dst mod src %1)
             (str/ends-with? (str/lower-case %2) "english.txt") (parse-english dst mod %1)
             (str/ends-with? (str/lower-case %2) "collections.txt") (parse-collections dst mod %1)
             :else %1)
          loaded-data
          (filter (fn [path] (some #(str/ends-with? (str/lower-case path) %) copy-files))
             (doall (map (fn [path] (do
                                      (. (. (io/file (str dst path)) getParentFile) mkdirs)
                                      (io/copy (io/file mod path) (io/file dst path))
                                      path))
                         (get-valid-files mod))))))

(defn apply-mods
  "Applies the given mods given the existing repair state (ENGLISH.TXT entries)"
  ([dst src mods loaded-data]
   (if (empty? mods)
     loaded-data
     (apply-mods dst src (rest mods) (apply-mod dst src (first mods) loaded-data))))
  ([dst src mods]
   (apply-mods dst src mods {:character-index 1675})))

(defn create-game-instance
  [source target]
  (do
     (when (.exists (io/file target)) (delete-directory-recursive (io/file target)))
     (create-link-tree source target)))

(defn -createLinkTree [src dst]
  (create-link-tree src dst))

(defn -createGameInstance [src dst]
  (create-game-instance src dst))

