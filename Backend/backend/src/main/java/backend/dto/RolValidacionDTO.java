package backend.dto;

public class RolValidacionDTO {
    private String rol;
    private boolean tienePanel;
    private Long clubId;

    public RolValidacionDTO(String rol, boolean tienePanel, Long clubId) {
        this.rol = rol;
        this.tienePanel = tienePanel;
        this.clubId = clubId;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isTienePanel() {
        return tienePanel;
    }

    public void setTienePanel(boolean tienePanel) {
        this.tienePanel = tienePanel;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }
}
