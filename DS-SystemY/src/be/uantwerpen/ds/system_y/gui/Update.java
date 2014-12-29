package be.uantwerpen.ds.system_y.gui;

public class Update extends Controller{

	public Update(Model model, View view) {
		super(model, view);
		view.setListModel(model.getList());
	}
}