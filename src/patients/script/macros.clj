(ns patients.script.macros)

(defmacro filter-input [field props] ;; multiple arity?
  `[patients.script.app/input (merge {:class "w-full"
                                      :type "text"
                                      :value (~field @patients.script.app/filters)
                                      :on-change #((swap! patients.script.app/filters assoc ~field (-> % .-target .-value))
                                                   (patients.script.app/get-list))} ~props)])

(defmacro form-input [field props]
  (let [patient  (&env 'patient)
        errors   (&env 'errors)
        validate (&env 'validate)]
    `[:div.p-0.m-0.flex.flex-col.justify-center
      [patients.script.app/input (merge {:class "m-3"
                                         :type "text"
                                         :value (~field @~patient)
                                         :on-change #((swap! @~patient assoc ~field (-> % .-target .-value))
                                                      (println (~field @~patient))
                                                      (~validate))} ~props)]
      [:div.text-sm.text-red-400 (~field @~errors)]]))
