import React, { useState } from 'react';
import cx from 'classnames';
import s from './Layout.module.scss';
import Header from '../Header/Header';
import Sidebar from '../Sidebar/Sidebar';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className={s.root}>
      <Sidebar />
      <div className={cx(s.wrap, { [s.sidebarOpen]: sidebarOpen })}>
        <Header sidebarToggle={() => setSidebarOpen(!sidebarOpen)} />
        <main className={s.content}>{children}</main>
      </div>
    </div>
  );
};

export default Layout;
