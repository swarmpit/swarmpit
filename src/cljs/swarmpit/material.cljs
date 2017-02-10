(ns swarmpit.material
  (:require [cljsjs.react]
            [cljsjs.material-ui]
            [sablono.core :refer-macros [html]]))

(def create-factory js/React.createFactory)
(def app-bar (create-factory js/MaterialUI.AppBar))
(def drawer (create-factory js/MaterialUI.Drawer))
(def menu-item (create-factory js/MaterialUI.MenuItem))
(def svg-icon (create-factory js/MaterialUI.SvgIcon))
(def icon-button (create-factory js/MaterialUI.IconButton))

(def auto-complete (create-factory js/MaterialUI.AutoComplete))
(def font-icon (create-factory js/MaterialUI.FontIcon))
(def mui-theme-provider (create-factory js/MaterialUIStyles.MuiThemeProvider))

(def auto-complete-filter js/MaterialUI.AutoComplete.caseInsensitiveFilter)
(def mui-theme js/MaterialUIStyles.getMuiTheme)
(def light-theme js/MaterialUIStyles.lightBaseTheme)
(def dark-theme js/MaterialUIStyles.darkBaseTheme)

(defn theme [comp]
  (let [default-theme (mui-theme light-theme)]
    [:div
     (mui-theme-provider #js{:muiTheme default-theme} comp)]))

(defn svg [d]
  (svg-icon #js {} (html [:path {:d d}])))

(def home-icon
  (svg "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"))

(def view-confy-icon
  (svg "M3 19h6v-7H3v7zm7 0h12v-7H10v7zM3 5v6h19V5H3z"))

(def view-compact-icon
  (svg "M3 9h4V5H3v4zm0 5h4v-4H3v4zm5 0h4v-4H8v4zm5 0h4v-4h-4v4zM8 9h4V5H8v4zm5-4v4h4V5h-4zm5 9h4v-4h-4v4zM3 19h4v-4H3v4zm5 0h4v-4H8v4zm5 0h4v-4h-4v4zm5 0h4v-4h-4v4zm0-14v4h4V5h-4z"))

(def repositories-icon
  (svg "M14 6H8v2h6V6zm4.006-4H5.994C4.894 2 4 2.89 4 3.99v16.02C4 21.102 4.894 22 5.994 22h12.012c1.1 0 1.994-.89 1.994-1.99V3.99C20 2.898 19.105 2 18.006 2zM18 20H6V4h12v16zm-5-10H8v2h5v-2z"))

(def stacks-icon
  (svg "M6 16v2h16v-2H6zM18 6H2v2h16V6zm2 5H4v2h16v-2z"))

(def services-icon
  (svg "M20.004 11H17V3.996c0-.54-.445-.996-.996-.996H7.996C7.456 3 7 3.446 7 3.996V11H3.996c-.54 0-.996.446-.996.996v8.008c0 .54.446.996.996.996h16.01c.54 0 .994-.445.994-.996v-8.008c0-.54-.445-.996-.996-.996zM9 5h6v6H9V5zm2 14H5v-6h6v6zm8 0h-6v-6h6v6zM9 15H7v2h2v-2zm2-6h2V7h-2v2zm6 6h-2v2h2v-2z"))

(def containers-icon
  (svg "M18 4H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H6V6h12v12zm-3-9H9v6h6V9z"))

(def nodes-icon
  (svg "M14.002 3H9.998C8.888 3 8 3.893 8 4.995v14.01C8 20.107 8.895 21 9.998 21h4.004c1.11 0 1.998-.893 1.998-1.995V4.995C16 3.893 15.105 3 14.002 3zM11 11c-.552 0-1-.448-1-1s.448-1 1-1 1 .448 1 1-.448 1-1 1zm0-4c-.552 0-1-.448-1-1s.448-1 1-1 1 .448 1 1-.448 1-1 1z"))