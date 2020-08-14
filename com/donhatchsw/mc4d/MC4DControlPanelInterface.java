/**
Interface for MC4DLegacyControlPanel and MC4DSwingControlPanel.
Note: in practice, every MC4DControlPanelInterface is also a Component
(either a Panel->Container->Component
or a JPanel->JComponent->Container->Component),
and the Component methods are used much more frequently than the
MC4DControlPanelInterface methods, so callers hold a Component variable
instead, and cast to MC4DControlPanelInterface in the relatively rare
cases when needed (i.e. when calling getViewParams()).
*/

package com.donhatchsw.mc4d;

interface MC4DControlPanelInterface
{
    public MC4DViewGuts.ViewParams getViewParams();
}
