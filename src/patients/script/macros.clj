(ns patients.script.macros)

(defmacro filter-input [field props]
  `[patients.script.app/input (merge {:class "w-full"
                                      :type "text"
                                      :value (~field @patients.script.app/filters)
                                      :on-change #((swap! patients.script.app/filters assoc ~field (-> % .-target .-value))
                                                   (patients.script.app/get-list))} ~props)])

(defmacro form-input [field props]
  `[:div.p-0.m-0.flex.flex-col.justify-center
    [patients.script.app/input (merge {:class "m-3"
                                       :type "text"
                                       :value (~field @patients.script.app/patient)
                                       :on-change #((swap! patients.script.app/patient assoc ~field (-> % .-target .-value))
                                                    (patients.script.app/validate))} ~props)]
    [:div.text-xs.text-red-400 (~field @patients.script.app/errors)]])
