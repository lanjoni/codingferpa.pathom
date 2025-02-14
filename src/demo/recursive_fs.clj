(ns demo.recursive-fs
  (:require [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [babashka.fs :as fs]))

(pco/defresolver path-children [{:fs/keys [path]}]
  {:fs/children (when (fs/directory? path)
                  (->> (fs/list-dir path)
                       (mapv #(array-map :fs/path (str %)))))})

(def env
  (-> {}
      (pci/register [path-children])))

(comment
  (p.eql/process env {:fs/path "/Users/joao.lanjoni/gh/codingferpa.pathom"}
                 [:fs/path {:fs/children '...}]))
