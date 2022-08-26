(ns patients.script.macros)

(defmacro filter-input [field props] ;; multiple arity?
  `[:input.filter (merge {:type "text"
                          :value (~field @patients.script.app/filters)
                          :on-change #((swap! patients.script.app/filters assoc ~field (-> % .-target .-value))
                                       (patients.script.app/get-list))} ~props)])
