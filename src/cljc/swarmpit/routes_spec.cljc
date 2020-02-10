(ns swarmpit.routes-spec
  (:require [spec-tools.data-spec :as ds]))

;; Parts

(def name-value
  {:name  string?
   :value string?})

(def resources
  {:cpu    number?
   :memory number?})

(def deploy
  {:parallelism   number?
   :delay         number?
   :order         string?
   :failureAction string?})

;; Apis

(def me
  {:username                   string?
   :role                       string?
   :api-token                  {:jti  string?
                                :mask string?}
   :email                      string?
   (ds/opt :node-dashboard)    [string?]
   (ds/opt :service-dashboard) [string?]})

(def stats
  {:hosts  number?
   :cpu    {:usage number?}
   :memory {:usage number?
            :used  number?
            :total number?}
   :disk   {:usage number?
            :used  number?
            :total number?}})

(def task
  {:id           string?
   :taskName     string?
   :version      number?
   :createdAt    string?
   :updatedAt    string?
   :repository   {:image       string?
                  :imageDigest string?}
   :state        string?
   :status       {:error string?}
   :desiredState string?
   :logdriver    string?
   :serviceName  string?
   :resources    {:reservation resources
                  :limit       resources}
   :nodeId       string?
   :nodeName     string?})

(def task-stats
  {:task    string?
   :service string?
   :time    [string?]
   :cpu     [number?]
   :memory  [number?]})

(def mount
  {:containerPath string?
   :host          string?
   :type          string?
   :id            string?
   :volumeOptions {:labels map?
                   :driver {:name    string?
                            :options any?}}
   :readOnly      boolean?
   :stack         string?})

(def port
  {:containerPort number?
   :mode          string?
   :protocol      string?
   :hostPort      number?})

(def config
  {:configName string?
   :createdAt  string?
   :data       string?
   :id         string?
   :updatedAt  string?
   :version    number?})

(def config-create
  {:configName string?
   :data       string?})

(def config-mount
  {:id           string?
   :configName   string?
   :configTarget string?
   :uid          number?
   :gid          number?
   :mode         number?})

(def secret
  {:secretName string?
   :createdAt  string?
   :id         string?
   :updatedAt  string?
   :version    number?})

(def secret-create
  {:secretName string?
   :data       string?})

(def secret-mount
  {:id           string?
   :secretName   string?
   :secretTarget string?
   :uid          number?
   :gid          number?
   :mode         number?})

(def network
  {:labels         map?
   :ingress        boolean?
   :enableIPv6     boolean?
   :created        string?
   :scope          string?
   :internal       boolean?
   :id             string?
   :ipam           {:subnet  string?
                    :gateway string?}
   :stack          string?
   :options        [{:name  string?
                     :value any?}]
   :networkName    string?
   :driver         string?
   :attachable     boolean?
   :serviceAliases [string?]})

(def network-create
  {:ingress     boolean?
   :enableIPv6  boolean?
   :internal    boolean?
   :ipam        {:subnet  string?
                 :gateway string?}
   :options     [name-value]
   :networkName string?
   :driver      string?
   :attachable  boolean?})

(def service
  {:id              string?
   :version         number?
   :createdAt       string?
   :updatedAt       string?
   :repository      {:name        string?
                     :tag         string?
                     :image       string?
                     :imageDigest string?}
   :serviceName     string?
   :mode            string?
   :replicas        number?
   :state           string?
   :status          {:tasks   {:running number?
                               :total   number?}
                     :update  string?
                     :message string?}
   :ports           [port]
   :mounts          [mount]
   :networks        [network]
   :secrets         [secret-mount]
   :configs         [config-mount]
   :hosts           [name-value]
   :variables       [name-value]
   :labels          [name-value]
   :containerLabels [name-value]
   :command         [string?]
   :stack           string?
   :agent           boolean?
   :links           [name-value]
   :user            string?
   :dir             string?
   :tty             boolean?
   :healthcheck     {:test     [string?]
                     :interval number?
                     :timeout  number?
                     :retries  number?}
   :logdriver       {:name string?
                     :opts [name-value]}
   :resources       {:reservation resources
                     :limit       resources}
   :deployment      {:update          deploy
                     :forceUpdate     number?
                     :restartPolicy   {:condition string?
                                       :delay     number?
                                       :window    number?
                                       :attempts  number?}
                     :rollback        deploy
                     :rollbackAllowed boolean?
                     :autoredeploy    boolean?
                     :placement       [{:rule string?}]}})

(def service-stats
  {:cpu    {:service string?
            :time    [string?]
            :cpu     [number?]}
   :memory {:service string?
            :time    [string?]
            :memory  [number?]}})

(def service-create
  {:repository        {:name string?
                       :tag  string?}
   :serviceName       string?
   :mode              string?
   (ds/opt :replicas) number?
   :ports             [{:containerPort number?
                        :protocol      string?
                        :mode          string?
                        :hostPort      number?}]
   :mounts            [{:containerPath string?
                        :host          string?
                        :type          string?
                        :readOnly      boolean?}]
   :networks          [{:networkName string?}]
   :secrets           [{:secretName   string?
                        :secretTarget string?}]
   :configs           [{:configName   string?
                        :configTarget string?}]
   :hosts             [name-value]
   :variables         [name-value]
   :labels            [name-value]
   (ds/opt :command)  [string?]
   :logdriver         {:name string?
                       :opts [name-value]}
   :resources         {:reservation resources
                       :limit       resources}
   :deployment        {:update        deploy
                       :restartPolicy {:condition       string?
                                       :delay           number?
                                       (ds/opt :window) number?
                                       :attempts        number?}
                       :rollback      deploy
                       :autoredeploy  boolean?
                       :placement     [{:rule string?}]}})

(def service-update
  {:repository               {:name string?
                              :tag  string?}
   :serviceName              string?
   :version                  number?
   :mode                     string?
   (ds/opt :replicas)        number?
   :ports                    [{:containerPort number?
                               :protocol      string?
                               :mode          string?
                               :hostPort      number?}]
   :mounts                   [{:containerPath          string?
                               :host                   string?
                               :type                   string?
                               :readOnly               boolean?
                               (ds/opt :volumeOptions) {(ds/opt :labels) map?
                                                        (ds/opt :driver) {:name             string?
                                                                          (ds/opt :options) [name-value]}}}]
   :networks                 [{:networkName             string?
                               (ds/opt :serviceAliases) [string?]}]
   :secrets                  [{:secretName   string?
                               :secretTarget string?}]
   :configs                  [{:configName   string?
                               :configTarget string?}]
   :hosts                    [name-value]
   :variables                [name-value]
   :labels                   [name-value]
   (ds/opt :containerLabels) [name-value]
   (ds/opt :command)         [string?]
   (ds/opt :stack)           string?
   (ds/opt :agent)           boolean?
   (ds/opt :immutable)       boolean?
   (ds/opt :links)           [name-value]
   (ds/opt :user)            string?
   (ds/opt :dir)             string?
   (ds/opt :tty)             boolean?
   (ds/opt :healthcheck)     {:test     [string?]
                              :interval number?
                              :timeout  number?
                              :retries  number?}
   :logdriver                {:name string?
                              :opts [name-value]}
   :resources                {:reservation resources
                              :limit       resources}
   :deployment               {:update        deploy
                              :restartPolicy {:condition       string?
                                              :delay           number?
                                              (ds/opt :window) number?
                                              :attempts        number?}
                              :rollback      deploy
                              :autoredeploy  boolean?
                              :placement     [{:rule string?}]}})

(def service-logs
  {:line      string?
   :timestamp string?
   :task      string?})

(def volume
  {:driver     string?
   :id         string?
   :labels     map?
   :mountpoint string?
   :options    [name-value]
   :scope      string?
   :stack      string?
   :volumeName string?
   :size       number?})

(def volume-create
  {:driver     string?
   :options    [name-value]
   :volumeName string?})

(def node
  {:id           string?
   :version      number?
   :nodeName     string?
   :role         string?
   :availability string?
   :labels       [name-value]
   :state        string?
   :address      string?
   :engine       string?
   :arch         string?
   :os           string?
   :resources    resources
   :plugins      {:networks [string?]
                  :volumes  [string?]}
   :leader       boolean?})

(def node-update
  {:nodeName     string?
   :version      number?
   :role         string?
   :availability string?
   :labels       [name-value]})

(def node-stats
  {:name   string?
   :time   [string?]
   :cpu    [number?]
   :memory [number?]})

(def repository
  {:name                 string?
   (ds/opt :description) string?
   (ds/opt :id)          number?
   (ds/opt :official)    string?
   (ds/opt :private)     boolean?
   (ds/opt :pulls)       number?
   (ds/opt :stars)       number?})

(def repositories
  {:query   string?
   :page    number?
   :limit   number?
   :total   number?
   :results [repository]})

(def user
  {(ds/opt :email)             string?
   (ds/opt :api-token)         (ds/maybe {:jti  string?
                                          :mask string?})
   (ds/opt :node-dashboard)    [string?]
   (ds/opt :service-dashboard) [string?]
   :role                       string?
   :type                       string?
   :username                   string?
   :_id                        string?
   :_rev                       string?})

(def user-create
  {(ds/opt :email) string?
   :password       string?
   :role           string?
   :username       string?})

(def user-update
  {(ds/opt :email)             string?
   (ds/opt :api-token)         (ds/maybe {:jti  string?
                                          :mask string?})
   (ds/opt :node-dashboard)    [string?]
   (ds/opt :service-dashboard) [string?]
   :role                       string?
   :type                       string?
   :username                   string?
   :_id                        string?
   :_rev                       string?})

(def stack
  {:configs   [config-mount]
   :secrets   [secret-mount]
   :networks  [network]
   :services  [service]
   :stackFile boolean?
   :stackName string?
   :volumes   [mount]})

(def stack-compose
  {:name string?
   :spec {:compose string?}})

(def stack-file
  {:name         string?
   :previousSpec {:compose string?}
   :spec         {:compose string?}
   :type         string?
   :_id          string?
   :_rev         string?})