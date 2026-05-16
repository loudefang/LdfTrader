export default function Navbar() {
  return (
    <nav className="app-navbar">
      <div className="brand">JZhu Trading</div>
      <div className="nav-links">
        <span className="nav-link active">K线回测</span>
        <span className="nav-link disabled">策略管理</span>
        <span className="nav-link disabled">实盘交易</span>
        <span className="nav-link disabled">账户</span>
      </div>
    </nav>
  );
}
