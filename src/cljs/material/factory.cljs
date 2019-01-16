(ns material.factory
  (:refer-clojure :exclude [list])
  (:require [cljsjs.react]
            [cljsjs.recharts]
            [cljsjs.react-select]
            [cljsjs.react-autosuggest]
            [cljsjs.rc-slider]
            [material-ui]
            [material-ui-icons]))

(set! *warn-on-infer* true)

(def create-factory js/React.createFactory)

;;; Material-UI

(def css-baseline (create-factory js/MaterialUI.CssBaseline))
(def mui-theme-provider (create-factory js/MaterialUI.MuiThemeProvider))

(def form-control (create-factory js/MaterialUI.FormControl))
(def form-label (create-factory js/MaterialUI.FormLabel))
(def form-control-label (create-factory js/MaterialUI.FormControlLabel))
(def form-group (create-factory js/MaterialUI.FormGroup))
(def form-helper-text (create-factory js/MaterialUI.FormHelperText))

(def input (create-factory js/MaterialUI.Input))
(def input-label (create-factory js/MaterialUI.InputLabel))
(def input-adornment (create-factory js/MaterialUI.InputAdornment))

(def list (create-factory js/MaterialUI.List))
(def list-subheader (create-factory js/MaterialUI.ListSubheader))
(def list-item (create-factory js/MaterialUI.ListItem))
(def list-item-text (create-factory js/MaterialUI.ListItemText))
(def list-item-secondary-action (create-factory js/MaterialUI.ListItemSecondaryAction))
(def list-item-icon (create-factory js/MaterialUI.ListItemIcon))

(def menu (create-factory js/MaterialUI.Menu))
(def menu-item (create-factory js/MaterialUI.MenuItem))
(def menu-list (create-factory js/MaterialUI.MenuList))

(def typography (create-factory js/MaterialUI.Typography))
(def drawer (create-factory js/MaterialUI.Drawer))
(def divider (create-factory js/MaterialUI.Divider))
(def toolbar (create-factory js/MaterialUI.Toolbar))
(def appbar (create-factory js/MaterialUI.AppBar))
(def avatar (create-factory js/MaterialUI.Avatar))
(def paper (create-factory js/MaterialUI.Paper))
(def hidden (create-factory js/MaterialUI.Hidden))
(def dialog (create-factory js/MaterialUI.Dialog))

(def chip (create-factory js/MaterialUI.Chip))
(def button (create-factory js/MaterialUI.Button))
(def icon-button (create-factory js/MaterialUI.IconButton))
(def svg-icon (create-factory js/MaterialUI.SvgIcon))
(def checkbox (create-factory js/MaterialUI.Checkbox))
(def select (create-factory js/MaterialUI.Select))
(def tooltip (create-factory js/MaterialUI.Tooltip))
(def snackbar (create-factory js/MaterialUI.Snackbar))
(def snackbar-content (create-factory js/MaterialUI.SnackbarContent))
(def text-field (create-factory js/MaterialUI.TextField))

(def table (create-factory js/MaterialUI.Table))
(def table-head (create-factory js/MaterialUI.TableHead))
(def table-cell (create-factory js/MaterialUI.TableCell))
(def table-row (create-factory js/MaterialUI.TableRow))
(def table-body (create-factory js/MaterialUI.TableBody))
(def table-footer (create-factory js/MaterialUI.TableFooter))

(def expansion-panel (create-factory js/MaterialUI.ExpansionPanel))
(def expansion-panel-summary (create-factory js/MaterialUI.ExpansionPanelSummary))
(def expansion-panel-details (create-factory js/MaterialUI.ExpansionPanelDetails))
(def expansion-panel-actions (create-factory js/MaterialUI.ExpansionPanelActions))

(def stepper (create-factory js/MaterialUI.Stepper))
(def step (create-factory js/MaterialUI.Step))
(def step-label (create-factory js/MaterialUI.StepLabel))
(def step-content (create-factory js/MaterialUI.StepContent))

(def grid (create-factory js/MaterialUI.Grid))

(def card (create-factory js/MaterialUI.Card))
(def card-header (create-factory js/MaterialUI.CardHeader))
(def card-content (create-factory js/MaterialUI.CardContent))
(def card-actions (create-factory js/MaterialUI.CardActions))
(def card-media (create-factory js/MaterialUI.CardMedia))

(def tab (create-factory js/MaterialUI.Tab))
(def tabs (create-factory js/MaterialUI.Tabs))

(def radio-group (create-factory js/MaterialUI.RadioGroup))
(def radio (create-factory js/MaterialUI.Radio))
(def switch (create-factory js/MaterialUI.Switch))

(def popper (create-factory js/MaterialUI.Popper))
(def grow (create-factory js/MaterialUI.Grow))
(def fade (create-factory js/MaterialUI.Fade))
(def no-ssr (create-factory js/MaterialUI.NoSsr))

(def linear-progress (create-factory js/MaterialUI.LinearProgress))
(def circular-progress (create-factory js/MaterialUI.CircularProgress))

(def click-away-listener (create-factory js/MaterialUI.ClickAwayListener))

;;; Material-UI Icons

(def visibility-icon (create-factory js/MaterialUIIcons.Visibility))
(def visibility-off-icon (create-factory js/MaterialUIIcons.VisibilityOff))
(def menu-icon (create-factory js/MaterialUIIcons.Menu))
(def chevron-left-icon (create-factory js/MaterialUIIcons.ChevronLeft))
(def chevron-right-icon (create-factory js/MaterialUIIcons.ChevronRight))
(def account-circle-icon (create-factory js/MaterialUIIcons.AccountCircle))
(def expand-more-icon (create-factory js/MaterialUIIcons.ExpandMore))
(def check-circle-icon (create-factory js/MaterialUIIcons.CheckCircle))
(def search-icon (create-factory js/MaterialUIIcons.Search))
(def key-icon (create-factory js/MaterialUIIcons.VpnKey))
(def settings-icon (create-factory js/MaterialUIIcons.Settings))
(def receipt-icon (create-factory js/MaterialUIIcons.Receipt))
(def storage-icon (create-factory js/MaterialUIIcons.Storage))
(def device-hub-icon (create-factory js/MaterialUIIcons.DeviceHub))
(def dns-icon (create-factory js/MaterialUIIcons.Dns))
(def error-icon (create-factory js/MaterialUIIcons.Error))
(def warning-icon (create-factory js/MaterialUIIcons.Warning))
(def cancel-icon (create-factory js/MaterialUIIcons.Cancel))
(def sync-icon (create-factory js/MaterialUIIcons.Sync))
(def access-time-icon (create-factory js/MaterialUIIcons.AccessTime))
(def fingerprint-icon (create-factory js/MaterialUIIcons.Fingerprint))
(def check-icon (create-factory js/MaterialUIIcons.Check))
(def close-icon (create-factory js/MaterialUIIcons.Close))
(def add-icon (create-factory js/MaterialUIIcons.Add))
(def add-circle-icon (create-factory js/MaterialUIIcons.AddCircle))
(def more-icon (create-factory js/MaterialUIIcons.MoreVert))
(def label-icon (create-factory js/MaterialUIIcons.Label))
(def info-icon (create-factory js/MaterialUIIcons.Info))
(def vertical-align-bottom-icon (create-factory js/MaterialUIIcons.VerticalAlignBottom))
(def logs-icon (create-factory js/MaterialUIIcons.Subject))
(def circle-icon (create-factory js/MaterialUIIcons.FiberManualRecord))
(def filter-list-icon (create-factory js/MaterialUIIcons.FilterList))
(def add-circle-out-icon (create-factory js/MaterialUIIcons.AddCircleOutline))
(def cloud-icon (create-factory js/MaterialUIIcons.Cloud))
(def storage-icon (create-factory js/MaterialUIIcons.Storage))
(def computer-icon (create-factory js/MaterialUIIcons.Computer))
(def share-icon (create-factory js/MaterialUIIcons.Share))
(def lock-icon (create-factory js/MaterialUIIcons.Lock))
(def open-in-new-icon (create-factory js/MaterialUIIcons.OpenInNew))

(def create-mui-theme js/MaterialUI.createMuiTheme)

;; Recharts

(def pie-chart (create-factory js/Recharts.PieChart))
(def pie (create-factory js/Recharts.Pie))
(def tooltip-chart (create-factory js/Recharts.Tooltip))
(def cell (create-factory js/Recharts.Cell))
(def legend (create-factory js/Recharts.Legend))
(def label (create-factory js/Recharts.Label))
(def responsive-container (create-factory js/Recharts.ResponsiveContainer))

;; React components

(def react-select (create-factory js/Select))
(def react-autosuggest (create-factory js/Autosuggest))
(def rc-slider (create-factory (.-default js/Slider)))
(def rc-slider-handle (create-factory js/Slider.Handle))
