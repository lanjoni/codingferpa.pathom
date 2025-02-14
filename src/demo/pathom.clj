(ns demo.pathom
  (:require [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.interface.eql :as p.eql]))

(def addresses
  {1 {:address/id 1
      :address/city "Indiaporã"
      :address/state "SP"}
   2 {:address/id 2
      :address/city "Cosmorama"
      :address/state "SP"}})

(def products
  {"Farinha" {:product/name "Farinha"
              :product/price 6
              :product/stock 10
              :product/description "Farinha de mandioca biju... mineirinha"
              :product/from {:address/id 1}}
   "Coxinha" {:product/name "Coxinha"
              :product/price 10
              :product/stock 20
              :product/description "Coxinha original da dona Irene"
              :product/from {:address/id 1}}
   "Arroz" {:product/name "Arroz"
            :product/price 3
            :product/stock 20
            :product/description "Arroz branco... agulhinha"
            :product/from {:address/id 2}}})

(pco/defresolver product-by-name [{:product/keys [name]}]
  {::pco/output [:product/description
                 :product/price
                 :product/stock
                 {:product/from [:address/id]}]}
  (get products name))

(pco/defresolver address-by-id [{:address/keys [id]}]
  {::pco/output [:address/city :address/state]}
  (get addresses id))

(pco/defresolver address-by-city [{:address/keys [city]}]
  {::pco/output [:address/id :address/state]}
  (->> addresses
       vals
       (filterv #(= city (-> % :address/city)))
       first))

(pco/defresolver products-by-address [{:address/keys [id]}]
  {::pco/output [{:address/products [:product/name]}]
   ::pco/cache? false}
  {:address/products (->> products
                          vals
                          (filter #(= id (-> % :product/from :address/id)))
                          (mapv (fn [p] (select-keys p [:product/name]))))})

(def env
  (-> {}
      (pci/register [address-by-id
                     address-by-city
                     product-by-name
                     products-by-address
                     (pbir/alias-resolver :product/box :product/name)])))

(comment
  ;; Bring the entire product entity by name
  (p.eql/process env {:product/name "Farinha"} [:product/price '*])

  ;; Why the example above is not working?
  (p.eql/process env {:product/name "Farinha"} [:product/name '*])
  ;; Because the resolver is not triggering as `:product/name` is our parameter

  ;; Bring just the city from the product filtered by name
  (p.eql/process env {:product/name "Farinha"} [{:product/from [:address/city]}])

  ;; Bring just the city from the product filtered by name
  ;; but with a different filter key by using an alias resolver
  ;; (box and name now are equivalents)
  (p.eql/process env {:product/box "Farinha"} [{:product/from [:address/city]}])

  ;; Bring all products from a city
  ;; (this one will use other resolver)
  (p.eql/process env {:address/id 1} [:address/products])

  ;; Find by id can be not very useful, so let's find by city!
  (p.eql/process env {:address/city "Indiaporã"} [:address/products])
  ;; We got only the names! Why? Because a new resolver wasn't triggered to get all of data!

  ;; Bring only product and description from a city
  (p.eql/process env {:address/city "Indiaporã"} [{:address/products [:product/price :product/description]}])

  ;; Bring all products from a city with all data
  (p.eql/process env {:address/city "Indiaporã"} [{:address/products [:product/price '*]}])
  ;; But wait... Then we also got `:address/id` again?

  ;; That means we can also bring the address data (again like a graph!)
  (p.eql/process env {:address/id 1} [{:address/products [:product/name
                                                          :product/price
                                                          {:product/from [:address/products]}]}])

  ;; And deeper...
  (p.eql/process env {:address/id 1} [{:address/products [:product/name
                                                          :product/price
                                                          {:product/from [{:address/products
                                                                           [{:product/from [:address/products]}]}]}]}]))
