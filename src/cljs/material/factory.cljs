(ns material.factory
  (:refer-clojure :exclude [list])
  (:require [cljsjs.react]
            [material-ui]
            [material-ui-icons]))

(def create-factory js/React.createFactory)

;;; Material-UI

(def css-baseline (create-factory js/MaterialUI.CssBaseline))
(def mui-theme-provider (create-factory js/MaterialUI.MuiThemeProvider))
(def form-control (create-factory js/MaterialUI.FormControl))
(def form-group (create-factory js/MaterialUI.FormGroup))
(def form-helper-text (create-factory js/MaterialUI.FormHelperText))
(def input (create-factory js/MaterialUI.Input))
(def input-label (create-factory js/MaterialUI.InputLabel))
(def input-adornment (create-factory js/MaterialUI.InputAdornment))
(def icon-button (create-factory js/MaterialUI.IconButton))
(def button (create-factory js/MaterialUI.Button))
(def typography (create-factory js/MaterialUI.Typography))
(def drawer (create-factory js/MaterialUI.Drawer))
(def divider (create-factory js/MaterialUI.Divider))
(def toolbar (create-factory js/MaterialUI.Toolbar))
(def appbar (create-factory js/MaterialUI.AppBar))
(def list-item (create-factory js/MaterialUI.ListItem))
(def list-item-text (create-factory js/MaterialUI.ListItemText))
(def list-item-icon (create-factory js/MaterialUI.ListItemIcon))
(def menu (create-factory js/MaterialUI.Menu))
(def menu-item (create-factory js/MaterialUI.MenuItem))
(def avatar (create-factory js/MaterialUI.Avatar))
(def paper (create-factory js/MaterialUI.Paper))
(def svg-icon (create-factory js/MaterialUI.SvgIcon))

(def visibility-icon (create-factory js/MaterialUIIcons.Visibility))
(def visibility-off-icon (create-factory js/MaterialUIIcons.VisibilityOff))
(def menu-icon (create-factory js/MaterialUIIcons.Menu))
(def chevron-left-icon (create-factory js/MaterialUIIcons.ChevronLeft))
(def chevron-right-icon (create-factory js/MaterialUIIcons.ChevronRight))
(def account-circle-icon (create-factory js/MaterialUIIcons.AccountCircle))

(def create-mui-theme js/MaterialUI.createMuiTheme)





(def dialog (create-factory js/MaterialUI.Dialog))
(def snackbar (create-factory js/MaterialUI.Snackbar))

(def icon-menu (create-factory js/MaterialUI.IconMenu))
(def flat-button (create-factory js/MaterialUI.FlatButton))
(def raised-button (create-factory js/MaterialUI.RaisedButton))
(def toogle (create-factory js/MaterialUI.Toggle))
(def checkbox (create-factory js/MaterialUI.Checkbox))
(def linear-progress (create-factory js/MaterialUI.LinearProgress))
(def circular-progress (create-factory js/MaterialUI.CircularProgress))
(def refresh-indicator (create-factory js/MaterialUI.RefreshIndicator))
(def table (create-factory js/MaterialUI.Table))
(def table-header (create-factory js/MaterialUI.TableHeader))
(def table-header-column (create-factory js/MaterialUI.TableHeaderColumn))
(def table-body (create-factory js/MaterialUI.TableBody))
(def table-row (create-factory js/MaterialUI.TableRow))
(def table-row-column (create-factory js/MaterialUI.TableRowColumn))
(def table-footer (create-factory js/MaterialUI.TableFooter))
(def tab (create-factory js/MaterialUI.Tab))
(def tabs (create-factory js/MaterialUI.Tabs))
(def select-field (create-factory js/MaterialUI.SelectField))
(def text-field (create-factory js/MaterialUI.TextField))
(def slider (create-factory js/MaterialUI.Slider))
(def radio-button-group (create-factory js/MaterialUI.RadioButtonGroup))
(def radio-button (create-factory js/MaterialUI.RadioButton))
(def auto-complete (create-factory js/MaterialUI.AutoComplete))
