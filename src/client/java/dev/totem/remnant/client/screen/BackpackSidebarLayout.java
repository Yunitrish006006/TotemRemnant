package dev.totem.remnant.client.screen;
/** Local semantic viewport; scrolling never reassigns a server slot index. */
public interface BackpackSidebarLayout {
    boolean remnant$contains(double x, double y);
    int remnant$left();
    int remnant$top();
    int remnant$firstRow();
    void remnant$firstRow(int row);
}
