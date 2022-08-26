(ns patients.script.macros)

(defmacro filter-input [field props] ;; multiple arity?
  `[:input.border-amber-700.w-full.border-2.rounded.px-2 (merge {:type "text"
                          :value (~field @patients.script.app/filters)
                          :on-change #((swap! patients.script.app/filters assoc ~field (-> % .-target .-value))
                                       (patients.script.app/get-list))} ~props)])
