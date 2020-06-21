(ns material.components
  (:refer-clojure :exclude [stepper list])
  (:require ["@material-ui/core"]
            ["react-window"]
            [cljsjs.recharts]
            [sablono.core :refer-macros [html]]
            [material.factory :refer [create-mui-cmp create-element create-js-element]]))

(def create-mui-theme js/MaterialUI.createMuiTheme)

(def create-mui-el (partial create-mui-cmp js/MaterialUI))
(def create-mui-lab-el (partial create-mui-cmp js/MaterialUILab))
(def create-rechart-el (partial create-mui-cmp js/Recharts))
(def create-rw-el (partial create-mui-cmp js/ReactWindow))

(defn theme-provider [& args] (create-mui-el "ThemeProvider" args))
(defn css-baseline [& args] (create-mui-el "CssBaseline" args))

;; Layout
(defn container [& args] (create-mui-el "Container" args))
(defn box [& args] (create-mui-el "Box" args))
(defn grid [& args] (create-mui-el "Grid" args))
(defn hidden [& args] (create-mui-el "Hidden" args))

;; Navigation
(defn drawer [& args] (create-mui-el "Drawer" args))
(defn swipeable-drawer [& args] (create-mui-el "SwipeableDrawer" args))
(defn menu [& args] (create-mui-el "Menu" args))
(defn menu-list [& args] (create-mui-el "MenuList" args))
(defn menu-item [& args] (create-mui-el "MenuItem" args))
(defn link [& args] (create-mui-el "Link" args))
(defn tabs [& args] (create-mui-el "Tabs" args))
(defn tab [& args] (create-mui-el "Tab" args))
(defn stepper [& args] (create-mui-el "Stepper" args))
(defn step [& args] (create-mui-el "Step" args))
(defn step-label [& args] (create-mui-el "StepLabel" args))
(defn step-content [& args] (create-mui-el "StepContent" args))

;; Surface
(defn app-bar [& args] (create-mui-el "AppBar" args))
(defn toolbar [& args] (create-mui-el "Toolbar" args))
(defn paper [& args] (create-mui-el "Paper" args))
(defn card [& args] (create-mui-el "Card" args))
(defn card-header [& args] (create-mui-el "CardHeader" args))
(defn card-content [& args] (create-mui-el "CardContent" args))
(defn card-actions [& args] (create-mui-el "CardActions" args))
(defn card-media [& args] (create-mui-el "CardMedia" args))

;; Feedback
(defn dialog [& args] (create-mui-el "Dialog" args))
(defn dialog-title [& args] (create-mui-el "DialogTitle" args))
(defn dialog-content [& args] (create-mui-el "DialogContent" args))
(defn dialog-actions [& args] (create-mui-el "DialogActions" args))
(defn circular-progress [& args] (create-mui-el "CircularProgress" args))
(defn linear-progress [& args] (create-mui-el "LinearProgress" args))
(defn snackbar [& args] (create-mui-el "Snackbar" args))
(defn snackbar-content [& args] (create-mui-el "SnackbarContent" args))
(defn backdrop [& args] (create-mui-el "Backdrop" args))

;; Data Display
(defn typography [& args] (create-mui-el "Typography" args))
(defn divider [& args] (create-mui-el "Divider" args))
(defn collapse [& args] (create-mui-el "Collapse" args))
(defn list [& args] (create-mui-el "List" args))
(defn list-item [& args] (create-mui-el "ListItem" args))
(defn list-item-text [& args] (create-mui-el "ListItemText" args))
(defn list-item-icon [& args] (create-mui-el "ListItemIcon" args))
(defn list-item-secondary-action [& args] (create-mui-el "ListItemSecondaryAction" args))
(defn table [& args] (create-mui-el "Table" args))
(defn table-head [& args] (create-mui-el "TableHead" args))
(defn table-body [& args] (create-mui-el "TableBody" args))
(defn table-cell [& args] (create-mui-el "TableCell" args))
(defn table-row [& args] (create-mui-el "TableRow" args))
(defn table-footer [& args] (create-mui-el "TableFooter" args))
(defn table-pagination [& args] (create-mui-el "TablePagination" args))
(defn icon-button [& args] (create-mui-el "IconButton" args))
(defn svg-icon [& args] (create-mui-el "SvgIcon" args))
(defn avatar [& args] (create-mui-el "Avatar" args))
(defn badge [& args] (create-mui-el "Badge" args))
(defn chip [& args] (create-mui-el "Chip" args))
(defn tooltip [& args] (create-mui-el "Tooltip" args))

(defn svg
  ([props d] (svg-icon props (html [:path {:d d}])))
  ([d] (svg-icon (html [:path {:d d}]))))

;; Inputs
(defn text-field [& args] (create-mui-el "TextField" args))
(defn select [& args] (create-mui-el "Select" args))
(defn input [& args] (create-mui-el "Input" args))
(defn input-base [& args] (create-mui-el "InputBase" args))
(defn input-adornment [& args] (create-mui-el "InputAdornment" args))
(defn button [& args] (create-mui-el "Button" args))
(defn button-group [& args] (create-mui-el "ButtonGroup" args))
(defn radio-group [& args] (create-mui-el "RadioGroup" args))
(defn radio [& args] (create-mui-el "Radio" args))
(defn form-group [& args] (create-mui-el "FormGroup" args))
(defn form-label [& args] (create-mui-el "FormLabel" args))
(defn form-control [& args] (create-mui-el "FormControl" args))
(defn form-control-label [& args] (create-mui-el "FormControlLabel" args))
(defn form-helper-text [& args] (create-mui-el "FormHelperText" args))
(defn slider [& args] (create-mui-el "Slider" args))
(defn switch [& args] (create-mui-el "Switch" args))
(defn checkbox [& args] (create-mui-el "Checkbox" args))
(defn fab [& args] (create-mui-el "Fab" args))

;; Special cases if parent ref must be passed to child (clj merge is loosing props)
(def text-field-js (partial create-js-element js/MaterialUI.TextField))

;; Utils
(defn popper [& args] (create-mui-el "Popper" args))
(defn popover [& args] (create-mui-el "Popover" args))
(defn fade [& args] (create-mui-el "Fade" args))
(defn grow [& args] (create-mui-el "Grow" args))
(defn no-ssr [& args] (create-mui-el "NoSsr" args))
(defn click-away-listener [& args] (create-mui-el "ClickAwayListener" args))

;; Lab
(defn toggle-button-group [& args] (create-mui-lab-el "ToggleButtonGroup" args))
(defn toggle-button [& args] (create-mui-lab-el "ToggleButton" args))
(defn skeleton [& args] (create-mui-lab-el "Skeleton" args))
(defn alert [& args] (create-mui-lab-el "Alert" args))
(defn speed-dial [& args] (create-mui-lab-el "SpeedDial" args))
(defn speed-dial-action [& args] (create-mui-lab-el "SpeedDialAction" args))
(defn speed-dial-icon [& args] (create-mui-lab-el "SpeedDialIcon" args))
(defn pagination [& args] (create-mui-lab-el "Pagination" args))
(defn autocomplete [& args] (create-mui-lab-el "Autocomplete" args))

;; Recharts
(defn pie-chart [& args] (create-rechart-el "PieChart" args))
(defn pie [& args] (create-rechart-el "Pie" args))
(defn tooltip-chart [& args] (create-rechart-el "Tooltip" args))
(defn cell [& args] (create-rechart-el "Cell" args))
(defn legend [& args] (create-rechart-el "Legend" args))
(defn re-label [& args] (create-rechart-el "Label" args))
(defn responsive-container [& args] (create-rechart-el "ResponsiveContainer" args))

;; Theme components
(def theme-typography
  {:h1        {:fontWeight    500
               :fontSize      "35px"
               :letterSpacing "-0.24px"
               :lineHeight    "40px"}
   :h2        {:fontWeight    300
               :fontSize      "35px"
               :letterSpacing "-0.24px"
               :lineHeight    "40px"}
   :h3        {:fontWeight    500
               :fontSize      "24px"
               :letterSpacing "-0.06px"
               :lineHeight    "28px"}
   :h4        {:fontWeight    500
               :fontSize      "20px"
               :letterSpacing "-0.06px"
               :lineHeight    "24px"}
   :h5        {:fontWeight    500
               :fontSize      "16px"
               :letterSpacing "-0.06px"
               :lineHeight    "20px"}
   :h6        {:fontWeight    500
               :fontSize      "14px"
               :letterSpacing "-0.05px"
               :lineHeight    "20px"}
   :subtitle1 {:fontSize      "16px"
               :letterSpacing "-0.05px"
               :lineHeight    "25px"}
   :subtitle2 {:fontWeight    400
               :fontSize      "14px"
               :letterSpacing "-0.05px"
               :lineHeight    "21px"}
   :body1     {:fontSize      "14px"
               :letterSpacing "-0.05px"
               :lineHeight    "21px"}
   :body2     {:fontSize      "12px"
               :letterSpacing "-0.04px"
               :lineHeight    "18px"}
   :caption   {:fontSize      "11px"
               :letterSpacing "0.33px"
               :lineHeight    "13px"}
   :overline  {:fontWeight    500
               :fontSize      "11px"
               :letterSpacing "0.33px"
               :lineHeight    "13px"
               :textTransform "uppercase"}})

(def theme-overrides
  {:MuiCardHeader  {:action {:color "rgb(117, 117, 117)"}}
   :MuiCardActions {:root {:padding 16}}
   :MuiPaper       {:elevation4 {:boxShadow "0px 4px 20px rgba(0, 0, 0, 0.15)"}}
   :MuiContainer   {:maxWidthMd {"@media (min-width: 1080px)" {:maxWidth (- 1080 48)}}}})

(def theme-breakpoints
  {:values {:xs 0
            :sm 600
            :md 1080
            :lg 1280
            :xl 1920}})

;; Light theme props
(def light-theme-props
  {:palette     {:primary   {:main         "#65519f"
                             :light        "#957ed1"
                             :dark         "#362870"
                             :contrastText "#fff"}
                 :secondary {:main "#8B9F51"}}
   :overrides   theme-overrides
   :breakpoints theme-breakpoints})

;; Dark theme props
(def dark-theme-props
  {:palette     {:primary   {:main         "#65519f"
                             :light        "#957ed1"
                             :dark         "#362870"
                             :contrastText "#fff"}
                 :secondary {:main "#8B9F51"}}
   :overrides   theme-overrides
   :breakpoints theme-breakpoints})

(def dark-theme
  (create-mui-theme (clj->js dark-theme-props)))

(def light-theme
  (create-mui-theme (clj->js light-theme-props)))

(defn theme [theme]
  (set! (-> js/document .-documentElement .-className) theme)
  (case theme
    "dark" dark-theme
    light-theme))

(defn mui [component]
  (theme-provider
    {:theme (theme "light")}
    (css-baseline)
    component))