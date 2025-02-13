(ns demo.pathom
  (:require [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]))

(def products
  {"Farinha" {:price 6
              :stock 10
              :description "Farinha de mandioca biju... mineirinha"
              :from {:city "Indiaporã"
                     :state "SP"}}
   "Coxinha" {:price 10
              :stock 20
              :description "Coxinha original da dona Irene"
              :from {:city "Indiaporã"
                     :state "SP"}}
   "Arroz" {:price 3
            :stock 20
            :description "Arroz branco... agulhinha"
            :from {:city "Cosmorama"
                   :state "SP"}}})

(pco/defresolver product-from-name [{:keys [name]}]
  {:product (get products name)})

(pco/defresolver product-from-city [{:keys [city]}]
  {:products (mapv (fn [p] {:product p})
                   (filter #(= city (-> % :from :city))
                           (vals products)))})

(def env
  (-> {}
      (pci/register [product-from-name
                     product-from-city
                     (pbir/alias-resolver :box :name)])))

(comment
  ;; Bring the entire product entity by name
  (p.eql/process env {:name "Farinha"} [:product])

  ;; Bring just the city from the product filtered by name
  (p.eql/process env {:name "Farinha"} [{:product
                                         [{:from [:city]}]}])

  ;; Bring just the city from the product filtered by name
  ;; but with a different filter key by using an alias resolver
  ;; (box and name now are equivalents)
  (p.eql/process env {:box "Farinha"} [{:product
                                        [{:from [:city]}]}])

  ;; Bring all products from a city
  ;; (this one will use the other resolver)
  (p.eql/process env {:city "Indiaporã"} [:products])

  ;; Bring all products description and price only from a city
  (p.eql/process env {:city "Indiaporã"} [{:products
                                           [{:product [:description :price]}]}]))
