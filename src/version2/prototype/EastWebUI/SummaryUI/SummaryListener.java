package version2.prototype.EastWebUI.SummaryUI;

import java.util.EventListener;

public interface SummaryListener extends EventListener {
    /** Called whenever the sun changes position
     *   in a SunEvent source object
     */
    public void AddSummary(SummaryEventObject e);
}
