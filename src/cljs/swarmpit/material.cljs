(ns swarmpit.material
  (:refer-clojure :exclude [stepper])
  (:require [cljsjs.react]
            [cljsjs.material-ui]
            [sablono.core :refer-macros [html]]))

(def home-icon "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")
(def view-confy-icon "M3 19h6v-7H3v7zm7 0h12v-7H10v7zM3 5v6h19V5H3z")
(def view-compact-icon "M3 9h4V5H3v4zm0 5h4v-4H3v4zm5 0h4v-4H8v4zm5 0h4v-4h-4v4zM8 9h4V5H8v4zm5-4v4h4V5h-4zm5 9h4v-4h-4v4zM3 19h4v-4H3v4zm5 0h4v-4H8v4zm5 0h4v-4h-4v4zm5 0h4v-4h-4v4zm0-14v4h4V5h-4z")
(def repositories-icon "M20.25 0h-18c-1.237 0-2.25 1.013-2.25 2.25v19.5c0 1.237 1.013 2.25 2.25 2.25h18c1.237 0 2.25-1.013 2.25-2.25v-19.5c0-1.237-1.013-2.25-2.25-2.25zM19.5 21h-16.5v-18h16.5v18zM6 10.5h10.5v1.5h-10.5zM6 13.5h10.5v1.5h-10.5zM6 16.5h10.5v1.5h-10.5zM6 7.5h10.5v1.5h-10.5z")
(def stacks-icon "M24 7.501l-12-6-12 6 12 6 12-6zM12 3.491l8.017 4.008-8.017 4.008-8.017-4.008 8.017-4.008zM21.597 10.798l2.403 1.202-12 6-12-6 2.403-1.202 9.597 4.798zM21.597 15.298l2.403 1.202-12 6-12-6 2.403-1.202 9.597 4.798z")
(def services-icon "M6.352 20.12l4.235-2.117v-3.462l-4.235 1.809v3.772zM5.646 15.11l4.456-1.908-4.456-1.908-4.456 1.908zM17.648 20.12l4.235-2.117v-3.462l-4.235 1.809v3.772zM16.941 15.11l4.456-1.908-4.456-1.908-4.456 1.908zM12 11.878l4.235-1.82v-2.933l-4.235 1.809v2.944zM11.294 7.698l4.864-2.084-4.864-2.084-4.864 2.084zM23.296 13.412v4.588q0 0.398-0.21 0.739t-0.574 0.519l-4.941 2.47q-0.277 0.154-0.628 0.154t-0.628-0.154l-4.941-2.47q-0.056-0.022-0.078-0.044-0.022 0.022-0.078 0.044l-4.941 2.47q-0.277 0.154-0.628 0.154t-0.628-0.154l-4.941-2.47q-0.364-0.177-0.574-0.519t-0.21-0.739v-4.588q0-0.42 0.237-0.771t0.623-0.53l4.786-2.051v-4.412q0-0.42 0.237-0.771t0.623-0.53l4.941-2.117q0.254-0.111 0.552-0.111t0.552 0.111l4.941 2.117q0.386 0.177 0.623 0.53t0.237 0.771v4.412l4.786 2.051q0.398 0.177 0.628 0.53t0.232 0.771z")
(def containers-icon "M12 21.817l8.571-4.674v-8.518l-8.571 3.121v10.071zM11.143 10.232l9.348-3.402-9.348-3.402-9.348 3.402zM22.286 6.857v10.286q0 0.469-0.241 0.871t-0.656 0.629l-9.429 5.143q-0.375 0.214-0.817 0.214t-0.817-0.214l-9.429-5.143q-0.415-0.228-0.656-0.629t-0.241-0.871v-10.286q0-0.536 0.308-0.978t0.817-0.629l9.429-3.429q0.295-0.107 0.589-0.107t0.589 0.107l9.429 3.429q0.509 0.188 0.817 0.629t0.308 0.978z")
(def nodes-icon "M1.714 18.857h13.714v-1.714h-13.714v1.714zM1.714 12h13.714v-1.714h-13.714v1.714zM22.714 18q0-0.536-0.375-0.911t-0.911-0.375-0.911 0.375-0.375 0.911 0.375 0.911 0.911 0.375 0.911-0.375 0.375-0.911zM1.714 5.143h13.714v-1.714h-13.714v1.714zM22.714 11.143q0-0.536-0.375-0.911t-0.911-0.375-0.911 0.375-0.375 0.911 0.375 0.911 0.911 0.375 0.911-0.375 0.375-0.911zM22.714 4.286q0-0.536-0.375-0.911t-0.911-0.375-0.911 0.375-0.375 0.911 0.375 0.911 0.911 0.375 0.911-0.375 0.375-0.911zM24 15.429v5.143h-24v-5.143h24zM24 8.571v5.143h-24v-5.143h24zM24 1.714v5.143h-24v-5.143h24z")
(def networks-icon "M22.875 18h-0.375v-4.875c0-1.448-1.178-2.625-2.625-2.625h-6.375v-3h0.375c0.619 0 1.125-0.506 1.125-1.125v-3.75c0-0.619-0.506-1.125-1.125-1.125h-3.75c-0.619 0-1.125 0.506-1.125 1.125v3.75c0 0.619 0.506 1.125 1.125 1.125h0.375v3h-6.375c-1.448 0-2.625 1.178-2.625 2.625v4.875h-0.375c-0.619 0-1.125 0.506-1.125 1.125v3.75c0 0.619 0.506 1.125 1.125 1.125h3.75c0.619 0 1.125-0.506 1.125-1.125v-3.75c0-0.619-0.506-1.125-1.125-1.125h-0.375v-4.5h6v4.5h-0.375c-0.619 0-1.125 0.506-1.125 1.125v3.75c0 0.619 0.506 1.125 1.125 1.125h3.75c0.619 0 1.125-0.506 1.125-1.125v-3.75c0-0.619-0.506-1.125-1.125-1.125h-0.375v-4.5h6v4.5h-0.375c-0.619 0-1.125 0.506-1.125 1.125v3.75c0 0.619 0.506 1.125 1.125 1.125h3.75c0.619 0 1.125-0.506 1.125-1.125v-3.75c0-0.619-0.506-1.125-1.125-1.125zM4.5 22.5h-3v-3h3v3zM13.5 22.5h-3v-3h3v3zM10.5 6v-3h3v3h-3zM22.5 22.5h-3v-3h3v3z")
(def trash-icon "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z")
(def plus-icon "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z")
(def pen-icon "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z")
(def plus2-icon "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")

(def create-factory js/React.createFactory)

(def app-bar (create-factory js/MaterialUI.AppBar))
(def drawer (create-factory js/MaterialUI.Drawer))
(def snackbar (create-factory js/MaterialUI.Snackbar))
(def menu (create-factory js/MaterialUI.Menu))
(def menu-item (create-factory js/MaterialUI.MenuItem))
(def svg-icon (create-factory js/MaterialUI.SvgIcon))
(def font-icon (create-factory js/MaterialUI.FontIcon))
(def icon-button (create-factory js/MaterialUI.IconButton))
(def flat-button (create-factory js/MaterialUI.FlatButton))
(def raised-button (create-factory js/MaterialUI.RaisedButton))
(def toogle (create-factory js/MaterialUI.Toggle))
(def checkbox (create-factory js/MaterialUI.Checkbox))
(def dialog (create-factory js/MaterialUI.Dialog))
(def circular-progress (create-factory js/MaterialUI.CircularProgress))
(def refresh-indicator (create-factory js/MaterialUI.RefreshIndicator))
(def table (create-factory js/MaterialUI.Table))
(def table-header (create-factory js/MaterialUI.TableHeader))
(def table-header-column (create-factory js/MaterialUI.TableHeaderColumn))
(def table-body (create-factory js/MaterialUI.TableBody))
(def table-row (create-factory js/MaterialUI.TableRow))
(def table-row-column (create-factory js/MaterialUI.TableRowColumn))
(def step (create-factory js/MaterialUI.Step))
(def stepper (create-factory js/MaterialUI.Stepper))
(def step-label (create-factory js/MaterialUI.StepLabel))
(def step-button (create-factory js/MaterialUI.StepButton))
(def select-field (create-factory js/MaterialUI.SelectField))
(def text-field (create-factory js/MaterialUI.TextField))
(def slider (create-factory js/MaterialUI.Slider))
(def radio-button-group (create-factory js/MaterialUI.RadioButtonGroup))
(def radio-button (create-factory js/MaterialUI.RadioButton))
(def auto-complete (create-factory js/MaterialUI.AutoComplete))
(def mui-theme-provider (create-factory js/MaterialUIStyles.MuiThemeProvider))

(def auto-complete-filter js/MaterialUI.AutoComplete.caseInsensitiveFilter)
(def mui-theme js/MaterialUIStyles.getMuiTheme)
(def fade js/MaterialUIUtils.colorManipulator.fade)

(def custom-theme
  #js {:palette #js {:primary1Color      "#437f9d"
                     :primary2Color      "#3C728D"
                     :primary3Color      "#bdbdbd"
                     :accent1Color       "#437f9d"
                     :accent2Color       "#f5f5f5"
                     :accent3Color       "#9e9e9e"
                     :textColor          "#757575"
                     :alternateTextColor "#ffffff"
                     :canvasColor        "#ffffff"
                     :borderColor        "#e0e0e0"
                     :disabledColor      (fade "rgba(0, 0, 0, 0.87)" 0.3)
                     :pickerHeaderColor  "437f9d"
                     :clockCircleColor   (fade "rgba(0, 0, 0, 0.87)" 0.07)
                     :shadowColor        "#000000"}})

(defn theme [comp]
  (let [default-theme (mui-theme custom-theme)]
    [:div
     (mui-theme-provider #js{:muiTheme default-theme} comp)]))

(defn svg
  ([props d] (svg-icon props (html [:path {:d d}])))
  ([d] (svg-icon nil (html [:path {:d d}]))))

(defn form-edit-row [label comp]
  [:div.form-edit-row
   [:span.form-row-label label]
   [:div.form-row-field (theme comp)]])

(defn form-view-row [label value]
  [:div.form-view-row
   [:span.form-row-label label]
   [:div.form-row-value value]])

(defn form-view-section [label]
  [:div.form-view-row
   [:span.form-section-label label]])

(defn table-header-list [headers]
  (table-header
    #js {:displaySelectAll  false
         :adjustForCheckbox false
         :style             #js {:border "none"}}
    (table-row
      #js {:displayBorder true}
      (map-indexed
        (fn [index header]
          (table-header-column #js {:key (str "header" index)} header))
        headers))))

(defn table-header-form [headers on-click-fn]
  (table-header
    #js {:displaySelectAll  false
         :adjustForCheckbox false
         :style             #js {:border "none"}}
    (table-row
      #js {:displayBorder false}
      (map-indexed
        (fn [index header]
          (table-header-column #js {:key (str "header" index)} header))
        headers)
      (table-header-column
        nil
        (icon-button
          #js {:onClick on-click-fn}
          (svg
            #js {:hoverColor "#437f9d"}
            plus-icon))))))

(defn table-row-form [index rows on-click-fn]
  (table-row
    #js {:key           (str "row" index)
         :rowNumber     index
         :displayBorder false}
    (map (fn [row] row) rows)
    (table-row-column
      #js {:key (str "delete-row" index)}
      (icon-button
        #js {:onClick on-click-fn}
        (svg
          #js {:hoverColor "red"}
          trash-icon)))))