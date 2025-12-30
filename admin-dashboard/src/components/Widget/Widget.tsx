import React from 'react';
import cx from 'classnames';
import s from './Widget.module.scss';

interface WidgetProps {
  title?: React.ReactNode;
  className?: string;
  children: React.ReactNode;
  actions?: React.ReactNode;
}

const Widget: React.FC<WidgetProps> = ({ title, className, children, actions }) => {
  return (
    <div className={cx(s.widget, className)}>
      {(title || actions) && (
        <div className={s.widgetHeader}>
          {title && (
            typeof title === 'string' ? (
              <h5 className={s.widgetTitle}>{title}</h5>
            ) : (
              <div className={s.widgetTitle}>{title}</div>
            )
          )}
          {actions && <div className={s.widgetActions}>{actions}</div>}
        </div>
      )}
      <div className={s.widgetBody}>{children}</div>
    </div>
  );
};

export default Widget;
